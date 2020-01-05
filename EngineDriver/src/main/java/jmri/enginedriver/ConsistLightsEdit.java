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
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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

//    private static final int WHICH_SOURCE_UNKNOWN = 0;
    private static final int WHICH_SOURCE_ADDRESS = 1;
    private static final int WHICH_SOURCE_ROSTER = 2;

    private ArrayList<ArrayList<Integer>> consistEngineAddressList = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> consistAddressSizeList = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> consistDirectionList = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> consistSourceList = new ArrayList<>();
    private ArrayList<ArrayList<String>> consistRosterNameList = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> consistLightList = new ArrayList<>();  // placeholder - not currently use
    private ArrayList<String> consistNameList = new ArrayList<>();

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

        loadRecentConsistsList();
        updateRecentConsists();

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

    // read the recent consists from a file
    // simliar, but different, code exists in select_loco.java, ImportExportPreferences.java and ConsistLightsEdit.java. if you modify one, make sure you modify the other
    private void loadRecentConsistsList() {
        consistEngineAddressList = new ArrayList<>();
        consistAddressSizeList = new ArrayList<>();
        consistDirectionList = new ArrayList<>();
        consistLightList = new ArrayList<>();
        consistSourceList = new ArrayList<>();
        consistRosterNameList = new ArrayList<>();
        consistNameList = new ArrayList<>();

        ArrayList<Integer> tempConsistEngineAddressList_inner;
        ArrayList<Integer> tempConsistAddressSizeList_inner;
        ArrayList<Integer> tempConsistDirectionList_inner;
        ArrayList<Integer> tempConsistSourceList_inner;
        ArrayList<String> tempConsistRosterNameList_inner;
        ArrayList<Integer> tempConsistLightList_inner;

        //if no SD Card present then there is no recent consists list
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent locos list requires SD Card
            TextView v = findViewById(R.id.recent_consists_heading);
            v.setText(getString(R.string.sl_recent_engine_notice));
        } else {
            try {
                // Populate the list with the recent consists saved in a file
                File sdcard_path = Environment.getExternalStorageDirectory();
                File consist_list_file = new File(sdcard_path + "/engine_driver/recent_consist_list.txt");
                if (consist_list_file.exists()) {
                    BufferedReader list_reader = new BufferedReader(
                            new FileReader(consist_list_file));
                    while (list_reader.ready()) {
                        StringBuilder oneConsist = new StringBuilder();
                        String line = list_reader.readLine();
                        tempConsistEngineAddressList_inner = new ArrayList<>();
                        tempConsistAddressSizeList_inner = new ArrayList<>();
                        tempConsistDirectionList_inner = new ArrayList<>();
                        tempConsistSourceList_inner = new ArrayList<>();
                        tempConsistRosterNameList_inner = new ArrayList<>();
                        tempConsistLightList_inner = new ArrayList<>();

                        String consistName = "";
                        String splitOn = "<~>";
                        if (!line.contains("<~>")) { // must be the old format
                            splitOn = "~";
                        }
                        String[] splitLine = line.split(splitOn, -1);

                        if (splitLine.length>1) { // see if there is a name saved as well
                            consistName = splitLine[1];
                            line = splitLine[0];
                            if (splitLine.length>2) {  // see if there is roster names saved as well
                                String[] rosterNames = splitLine[2].split("<,>", -1);
                                tempConsistRosterNameList_inner.addAll(Arrays.asList(rosterNames));
                            }
                        }

                        int splitLoco = line.indexOf(',');
                        if (splitLoco!=-1) {
                            oneConsist.append(mainapp.addOneConsistAddress(line, 0, splitLoco,
                                    tempConsistEngineAddressList_inner, tempConsistAddressSizeList_inner,
                                    tempConsistDirectionList_inner,
                                    tempConsistSourceList_inner,
                                    tempConsistLightList_inner));

                            boolean foundOne = true;
                            while (foundOne) {
                                Integer prevSplitLoco = splitLoco + 1;
                                splitLoco = line.indexOf(',', prevSplitLoco);
                                if (splitLoco != -1) {
                                    oneConsist.append(mainapp.addOneConsistAddress(line, prevSplitLoco, splitLoco,
                                            tempConsistEngineAddressList_inner, tempConsistAddressSizeList_inner,
                                            tempConsistDirectionList_inner,
                                            tempConsistSourceList_inner,
                                            tempConsistLightList_inner));
                                } else {
                                    oneConsist.append(mainapp.addOneConsistAddress(line, prevSplitLoco, line.length(),
                                            tempConsistEngineAddressList_inner, tempConsistAddressSizeList_inner,
                                            tempConsistDirectionList_inner,
                                            tempConsistSourceList_inner,
                                            tempConsistLightList_inner));
                                    foundOne = false;
                                }
                            }
                            if (splitLine.length<3) {  // old format - need to add some dummy roster names
                                for (int j=0; j<tempConsistEngineAddressList_inner.size(); j++) {
                                    tempConsistRosterNameList_inner.add("");
                                }
                            }
                            consistEngineAddressList.add(tempConsistEngineAddressList_inner);
                            consistAddressSizeList.add(tempConsistAddressSizeList_inner);
                            consistDirectionList.add(tempConsistDirectionList_inner);
                            consistSourceList.add(tempConsistSourceList_inner);
                            consistRosterNameList.add(tempConsistRosterNameList_inner);
                            consistLightList.add(tempConsistLightList_inner);
                            if (consistName.length()==0) { consistName = oneConsist.toString(); }
                            consistNameList.add(consistName);
                        }
                    }
                    list_reader.close();
                }

            } catch (IOException except) {
                Log.e("Engine_Driver", "ConsistLightsEdit - Error reading recent loco file. "
                        + except.getMessage());
            }
        }
    }



    // write the recent consists to a file
    // simliar, but different, code exists in select_loco.java, ImportExportPreferences.java and ConsistLightsEdit.java. if you modify one, make sure you modify the other
    void updateRecentConsists() {
        ArrayList<Integer> tempConsistEngineAddressList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistAddressSizeList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistDirectionList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistSourceList_inner = new ArrayList<>();
        ArrayList<String> tempConsistRosterNameList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistLightList_inner = new ArrayList<>();

        //if not updating list or no SD Card present then nothing else to do
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;

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
        for (int i = 0; i < consistEngineAddressList.size(); i++) {
            if (consistEngineAddressList.get(i).size() == tempConsistEngineAddressList_inner.size()) {  // if the lists are different sizes don't bother
                boolean isSame = true;
                for (int j = 0; j < consistEngineAddressList.get(i).size() && isSame; j++) {
                    if ((!consistEngineAddressList.get(i).get(j).equals(tempConsistEngineAddressList_inner.get(j)))
                            || (!consistDirectionList.get(i).get(j).equals(tempConsistDirectionList_inner.get(j)))) {
                        isSame = false;
                    }
                }
                if (isSame) {
                    whichEntryIsBeingUpdated = i + 1; //remember this, so we can remove this line in the list.  Add 1 because we are gone to force a new line at the top
                }
            }
        }

        //add it to the beginning of the list
        consistEngineAddressList.add(0, tempConsistEngineAddressList_inner);
        consistAddressSizeList.add(0, tempConsistAddressSizeList_inner);
        consistDirectionList.add(0, tempConsistDirectionList_inner);
        consistSourceList.add(0, tempConsistSourceList_inner);
        consistRosterNameList.add(0, tempConsistRosterNameList_inner);
        consistLightList.add(0, tempConsistLightList_inner);

        String consistName = consist.toString();
        if (whichEntryIsBeingUpdated>0) { //this may already have a name
            consistName = consistNameList.get(whichEntryIsBeingUpdated-1);
        }
        consistNameList.add(0, consistName);


        // Save the consist list to the recent_consist_list.txt file
        File sdcard_path = Environment.getExternalStorageDirectory();
        File connections_list_file = new File(sdcard_path,
                "engine_driver/recent_consist_list.txt");
        PrintWriter list_output;
        int numberOfRecentLocosToWrite = preferences.getIntPrefValue(prefs, "maximum_recent_locos_preference", getApplicationContext().getResources().getString(R.string.prefMaximumRecentLocosDefaultValue));
        try {
            list_output = new PrintWriter(connections_list_file);
            if (numberOfRecentLocosToWrite > 0) {
                numberOfRecentLocosToWrite--;

                for (int i = 0; i < consistEngineAddressList.size() && numberOfRecentLocosToWrite > 0; i++) {

                    if (i!=whichEntryIsBeingUpdated) { // if this is the one being updated, don't write it
                        for (int j = 0; j < consistAddressSizeList.get(i).size(); j++) {
                            if (j > 0) {
                                list_output.format(",");
                            }
                            list_output.format("%d:%d%d%d%d",
                                    consistEngineAddressList.get(i).get(j),
                                    consistAddressSizeList.get(i).get(j),
                                    consistDirectionList.get(i).get(j),
                                    consistSourceList.get(i).get(j),
                                    (j==0 ? LIGHT_FOLLOW :consistLightList.get(i).get(j)) );  // always set the first loco as 'follow'
                        }
                        list_output.format("<~>%s<~>", consistNameList.get(i));
                        for (int j = 0; j < consistRosterNameList.get(i).size(); j++) {
                            if (j > 0) {
                                list_output.format("<,>");
                            }
                            list_output.format("%s",
                                    consistRosterNameList.get(i).get(j));
                        }

                        list_output.format("\n");
                        numberOfRecentLocosToWrite--;
                    }
                }
            }
            list_output.flush();
            list_output.close();
        } catch (IOException except) {
            Log.e("Engine_Driver",
                    "ConsistLightsEdit - Error creating a PrintWriter, IOException: "
                            + except.getMessage());
        }

    }

}