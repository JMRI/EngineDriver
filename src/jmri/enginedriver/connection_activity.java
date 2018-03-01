/* Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

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
import android.os.AsyncTask;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.widget.SimpleAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.File;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.IOException;

import android.util.DisplayMetrics;
import android.util.Log;

import java.io.FileReader;
import java.lang.reflect.Method;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.os.Message;
import android.widget.EditText;
import android.widget.Button;
import android.os.Handler;
import android.provider.Settings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.AdapterView;

public class connection_activity extends Activity {
    private ArrayList<HashMap<String, String>> connections_list;
    private ArrayList<HashMap<String, String>> discovery_list;
    private SimpleAdapter connection_list_adapter;
    private SimpleAdapter discovery_list_adapter;
    private SharedPreferences prefs;

    //pointer back to application
    private threaded_application mainapp;
    //The IP address and port that are used to connect.
    private String connected_hostip;
    private String connected_hostname;
    private int connected_port;
    private boolean navigatingAway = false;        // flag for onPause: set to true when another activity is selected, false if going into background

    private String deviceId = "";

    private static final String demo_host = "jmri.mstevetodd.com";
    private static final String demo_port = "44444";

    private boolean prefHideDemoServer = false;

    private static Method overridePendingTransition;

    private ArrayList<Integer> engine_address_list;
    private ArrayList<Integer> address_size_list; // Look at address_type.java
    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    private static String AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT = "Connect Disconnect";
    private static String AUTO_IMPORT_EXPORT_OPTION_CONNECT_ONLY = "Connect Only";


        static {
        try {
            overridePendingTransition = Activity.class.getMethod("overridePendingTransition", Integer.TYPE, Integer.TYPE); //$NON-NLS-1$
        } catch (NoSuchMethodException e) {
            overridePendingTransition = null;
        }
    }

    /**
     * Calls Activity.overridePendingTransition if the method is available (>=Android 2.0)
     *
     * @param activity  the activity that launches another activity
     * @param animEnter the entering animation
     * @param animExit  the exiting animation
     */
    public static void overridePendingTransition(Activity activity, int animEnter, int animExit) {
        if (overridePendingTransition != null) {
            try {
                overridePendingTransition.invoke(activity, animEnter, animExit);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    //Request connection to the WiThrottle server.
    private void connect() {
        //	  sendMsgErr(0, message_type.CONNECT, connected_hostip, connected_port, "ERROR in ca.connect: comm thread not started.");
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CONNECT, connected_hostip, connected_port);
    }


    private void start_throttle_activity() {
        Intent throttle = new Intent().setClass(this, throttle.class);
        navigatingAway = true;
        startActivity(throttle);
        this.finish();
        overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private enum server_list_type {DISCOVERED_SERVER, RECENT_CONNECTION}

    private class connect_item implements AdapterView.OnItemClickListener {
        final server_list_type server_type;

        connect_item(server_list_type new_type) {
            server_type = new_type;
        }

        //When an item is clicked, connect to the given IP address and port.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            switch (server_type) {
                case DISCOVERED_SERVER:
                case RECENT_CONNECTION:
                    ViewGroup vg = (ViewGroup) v; //convert to viewgroup for clicked row
                    TextView hip = (TextView) vg.getChildAt(0); // get host ip from 1st box
                    connected_hostip = hip.getText().toString();
                    TextView hnv = (TextView) vg.getChildAt(1); // get host name from 2nd box
                    connected_hostname = hnv.getText().toString();
                    TextView hpv = (TextView) vg.getChildAt(2); // get port from 3rd box
                    connected_port = Integer.valueOf(hpv.getText().toString());
                    break;
            }
            connect();
        }
    }

    private class button_listener implements View.OnClickListener {
        public void onClick(View v) {
            EditText entry = (EditText) findViewById(R.id.host_ip);
            connected_hostip = entry.getText().toString();
            if (connected_hostip.trim().length() > 0) {
                entry = (EditText) findViewById(R.id.port);
                try {
                    connected_port = Integer.valueOf(entry.getText().toString());
                } catch (Exception except) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectInvalidPort) + "\n" + except.getMessage(), Toast.LENGTH_SHORT).show();
                    connected_port = 0;
                    return;
                }
                connected_hostname = connected_hostip; //copy ip to name
                connect();
            } else {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectEnterAddress), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Method to connect to first discovered WiThrottle server.
    private void connectA() {
        try {
            if (discovery_list.get(0) != null) {
                HashMap<String, String> tm = discovery_list.get(0);

                connected_hostip = tm.get("ip_address");

                if (connected_hostip.trim().length() > 0) {
                    try {
                        connected_port = Integer.valueOf(tm.get("port"));
                    } catch (Exception except) {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectInvalidPort) + "\n" + except.getMessage(), Toast.LENGTH_SHORT).show();
                        connected_port = 0;
                        return;
                    }
                    connected_hostname = tm.get("host_name"); //copy ip to name
                    connect();
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectConnected).replace("%%1%%",connected_hostname).replace("%%2%%", Integer.toString(connected_port)), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectEnterAddress), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ex) {
            //Catches exception, if no discovered server exists.
            //Does Nothing On purpose.
        }
    }

    //Handle messages from the communication thread back to the UI thread.
    @SuppressLint("HandlerLeak")
    private class ui_handler extends Handler {
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.SERVICE_RESOLVED:
                    HashMap<String, String> hm = (HashMap<String, String>) msg.obj;  //payload is already a hashmap
                    String found_host_name = hm.get("host_name");
                    boolean entryExists = false;

                    //stop if new address is already in the list
                    HashMap<String, String> tm;
                    for (int index = 0; index < discovery_list.size(); index++) {
                        tm = discovery_list.get(index);
                        if (tm.get("host_name").equals(found_host_name)) {
                            entryExists = true;
                            break;
                        }
                    }
                    if (!entryExists) {                // if new host, add to discovered list on screen
                        discovery_list.add(hm);
                        discovery_list_adapter.notifyDataSetChanged();

                        if (prefs.getBoolean("connect_to_first_server_preference", false)) {
                            connectA();
                        }
                    }
                    break;

                case message_type.SERVICE_REMOVED:
                    //look for name in list
                    String removed_host_name = msg.obj.toString();
                    for (int index = 0; index < discovery_list.size(); index++) {
                        tm = discovery_list.get(index);
                        if (tm.get("host_name").equals(removed_host_name)) {
                            discovery_list.remove(index);
                            discovery_list_adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                    break;

                case message_type.CONNECTED:
                    //use asynctask to save the updated connections list to the connections_list.txt file
                    new saveConnectionsList().execute();
                    mainapp.connectedHostName = connected_hostname;
                    loadSharedPreferencesFromFile();

                    start_throttle_activity();
                    break;

                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    saveSharedPreferencesToFile();
                    mainapp.connectedHostName = "";
                    shutdown();
                    break;
            }
        }
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //    timestamp = SystemClock.uptimeMillis();
        Log.d("Engine_Driver", "connection.onCreate ");
        mainapp = (threaded_application) this.getApplication();
        mainapp.connection_msg_handler = new ui_handler();
        // setTitle(getApplicationContext().getResources().getString(R.string.app_name_connect));	//set title to long form of label

        //ensure statics in all activities are reinitialize since Android might not have killed app since it was last Exited.
        //do this here instead of TA.onCreate() because that method won't be invoked if app is still running.
        throttle.initStatics();
        web_activity.initStatics();


        //check for "default" throttle name and make it more unique
        //TODO: move this and similar code in preferences.java into single routine

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
        String s = prefs.getString("throttle_name_preference", defaultName);
        if (s.trim().equals("") || s.equals(defaultName)) {
            String deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
            if (deviceId != null && deviceId.length() >= 4) {
                deviceId = deviceId.substring(deviceId.length() - 4);
            } else {
                Random rand = new Random();
                deviceId = String.valueOf(rand.nextInt(9999));  //use random string
            }
            s = defaultName + " " + deviceId;
            prefs.edit().putString("throttle_name_preference", s).commit();  //save new name to prefs

        }

        mainapp.applyTheme(this);

        setContentView(R.layout.connection);

        prefHideDemoServer = prefs.getBoolean("prefHideDemoServer", getResources().getBoolean(R.bool.prefHideDemoServerDefaultValue));


        //Set up a list adapter to allow adding discovered WiThrottle servers to the UI.
        discovery_list = new ArrayList<>();
        discovery_list_adapter = new SimpleAdapter(this, discovery_list, R.layout.connections_list_item,
                new String[]{"ip_address", "host_name", "port"},
                new int[]{R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
        ListView discover_list = (ListView) findViewById(R.id.discovery_list);
        discover_list.setAdapter(discovery_list_adapter);
        discover_list.setOnItemClickListener(new connect_item(server_list_type.DISCOVERED_SERVER));

        //Set up a list adapter to allow adding the list of recent connections to the UI.
        connections_list = new ArrayList<>();
        connection_list_adapter = new SimpleAdapter(this, connections_list, R.layout.connections_list_item,
                new String[]{"ip_address", "host_name", "port"},
                new int[]{R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
        ListView conn_list = (ListView) findViewById(R.id.connections_list);
        conn_list.setAdapter(connection_list_adapter);
        conn_list.setOnItemClickListener(new connect_item(server_list_type.RECENT_CONNECTION));

        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Set the button callback.
        Button button = (Button) findViewById(R.id.connect);
        button_listener click_listener = new button_listener();
        button.setOnClickListener(click_listener);

        set_labels();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        threaded_application.min_fling_distance = (int) (threaded_application.SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
        threaded_application.min_fling_velocity = (int) (threaded_application.SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);
    }

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (this.isFinishing()) {        //if finishing, expedite it
            return;
        }

        navigatingAway = false;
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        //start up server discovery listener
        //	    sendMsgErr(0, message_type.SET_LISTENER, "", 1, "ERROR in ca.onResume: comm thread not started.") ;
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 1);
        //populate the ListView with the recent connections
        getConnectionsList();
        set_labels();
        mainapp.cancelForcingFinish();            // if fresh start are restart after being killed in the bkg, indicate app is running again
        //start up server discovery listener again (after a 1 second delay)
        //TODO: this is a rig, figure out why this is needed for ubuntu servers
        //	    sendMsgErr(1000, message_type.SET_LISTENER, "", 1, "ERROR in ca.onResume: comm thread not started.") ;
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 1);

        if (prefs.getBoolean("connect_to_first_server_preference", false)) {
            connectA();
        }
    }  //end of onResume

    @Override
    public void onPause() {
        super.onPause();
        if (!this.isFinishing() && !navigatingAway) {        //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    @Override
    public void onStop() {
        Log.d("Engine_Driver", "connection.onStop()");
        //shutdown server discovery listener
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 0);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "connection.onDestroy()");
        //		mainapp.connection_msg_handler = null;
        super.onDestroy();
    }

    private void shutdown() {
        this.finish();
    }

    private void set_labels() {
        SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        TextView v = (TextView) findViewById(R.id.ca_footer);
        String s = prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
        v.setText(getString(R.string.throttle_name, s));

        //sets the tile to include throttle name.
        //String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_connect));// + "    |    Throttle Name: " + prefs.getString("throttle_name_preference", defaultName));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.connection_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        switch (item.getItemId()) {
            case R.id.exit_mnu:
                mainapp.checkExit(this);
                break;
            case R.id.preferences_mnu:
                in = new Intent().setClass(this, preferences.class);
                navigatingAway = true;
                startActivityForResult(in, 0);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.about_mnu:
                in = new Intent().setClass(this, about_page.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.ClearconnList:
                clearConnectionsList();
                getConnectionsList();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //handle return from menu items
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //only one activity with results here
        set_labels();
    }

    // Handle pressing of the back button to request exit
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            mainapp.checkExit(this);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    /***
     private void sendMsgErr(long delayMs, int msgType, String msgBody, int msgArg1, String errMsg) {
     if(!mainapp.sendMsgDelay(mainapp.comm_msg_handler, delayMs, msgType, msgBody, msgArg1, 0)) {
     Log.e("Engine_Driver",errMsg) ;
     Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
     }
     }
     ***/

    //save connections list in background using asynctask
    private class saveConnectionsList extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String errMsg = "";

            //if no SD Card present then nothing to do
            if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
                return errMsg;
            try {
                File path = Environment.getExternalStorageDirectory();
                File engine_driver_dir = new File(path, "engine_driver");
                //noinspection ResultOfMethodCallIgnored
                engine_driver_dir.mkdir();            // create directory if it doesn't exist

                File connections_list_file = new File(path, "engine_driver/connections_list.txt");
                PrintWriter list_output = new PrintWriter(connections_list_file);

                //Write selected connection to file, then write all others (skipping selected if found)
                list_output.format("%s:%s:%d\n", connected_hostname, connected_hostip, connected_port);

                SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
                String smrc = prefs.getString("maximum_recent_connections_preference", ""); //retrieve pref for max recents to show
                if (smrc.equals("")) { //if no value or entry removed, set to default
                    smrc = getApplicationContext().getResources().getString(R.string.prefMaximumRecentConnectionsDefaultValue);
                }
                int mrc = 10;
                try {
                    mrc = Integer.parseInt(smrc);
                } catch (NumberFormatException ignored) {
                }
                int clEntries = Math.min(connections_list.size(), mrc);  //don't keep more entries than specified in preference
                for (int i = 0; i < clEntries; i++) {  //loop thru entries from connections list, up to max in prefs
                    HashMap<String, String> t = connections_list.get(i);
                    String li = t.get("ip_address");
                    String lh = t.get("host_name");
                    Integer lp = Integer.valueOf(t.get("port"));
                    //***        			if(connected_hostip != null && connected_port != 0)
                    if (!connected_hostip.equals(li) || connected_port != lp) {  //write it out if not same as selected
                        list_output.format("%s:%s:%d\n", lh, li, lp);
                    }
                }
                list_output.flush();
                list_output.close();
            } catch (IOException except) {
                errMsg = except.getMessage();
            }
            return errMsg;
        }

        @Override
        protected void onPostExecute(String errMsg) {
            if (errMsg.length() > 0)
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectErrorSavingRecentConnection) + " " + errMsg, Toast.LENGTH_SHORT).show();
        }
    }

    //Jeffrey M added 7/3/2013
    //Clears recent connection list.
    private void clearConnectionsList() {
        //if no SD Card present then nothing to do
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getApplicationContext(), "Error no recent connections exist", Toast.LENGTH_SHORT).show();
        } else {
            File sdcard_path = Environment.getExternalStorageDirectory();
            File connections_list_file = new File(sdcard_path, "engine_driver/connections_list.txt");

            if (connections_list_file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                connections_list_file.delete();
                connections_list.clear();
            }
        }
    }

    private void getConnectionsList() {
        boolean foundDemoHost = false;
        connections_list.clear();
        String errMsg;
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent connections list requires SD Card
            TextView v = (TextView) findViewById(R.id.recent_connections_heading);
            v.setText(getString(R.string.ca_recent_conn_notice));
        } else {
            try {
                File sdcard_path = Environment.getExternalStorageDirectory();
                File connections_list_file = new File(sdcard_path, "engine_driver/connections_list.txt");

                if (connections_list_file.exists()) {
                    BufferedReader list_reader = new BufferedReader(new FileReader(connections_list_file));
                    while (list_reader.ready()) {
                        String line = list_reader.readLine();
                        String ip_address;
                        String host_name;
                        String port_str;
                        Integer port = 0;
                        List<String> parts = Arrays.asList(line.split(":", 3)); //split record from file, max of 3 parts
                        if (parts.size() > 1) {  //skip if not split
                            if (parts.size() == 2) {  //old style, use part 1 for ip and host
                                host_name = parts.get(0);
                                ip_address = parts.get(0);
                                port_str = parts.get(1);
                            } else {                          //new style, get all 3 parts
                                host_name = parts.get(0);
                                ip_address = parts.get(1);
                                port_str = parts.get(2);
                            }
                            try {  //attempt to convert port to integer
                                port = Integer.decode(port_str);
                            } catch (Exception ignored) {
                            }
                            if (port > 0) {  //skip if port not converted to integer

                                if ((!prefHideDemoServer)
                                        || ((prefHideDemoServer)  && !((host_name.equals(demo_host)) && (port.toString().equals(demo_port))))) {
                                    HashMap<String, String> hm = new HashMap<>();
                                    hm.put("ip_address", ip_address);
                                    hm.put("host_name", host_name);
                                    hm.put("port", port.toString());
                                    if (!connections_list.contains(hm)) {    // suppress dups
                                        connections_list.add(hm);
                                    }
                                }
                                if (host_name.equals(demo_host) && port.toString().equals(demo_port)) {
                                    foundDemoHost = true;
                                }
                            }
                        }
                    }
                    list_reader.close();
                }
            } catch (IOException except) {
                errMsg = except.getMessage();
                Log.e("connection_activity", "Error reading recent connections list: " + errMsg);
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectErrorReadingRecentConnections) + " " + errMsg, Toast.LENGTH_SHORT).show();
            }
        }

        //if demo host not already in list, add it at end
        if ((!prefHideDemoServer) && (!foundDemoHost)) {
            HashMap<String, String> hm = new HashMap<>();
            hm.put("ip_address", demo_host);
            hm.put("host_name", demo_host);
            hm.put("port", demo_port);
            connections_list.add(hm);
        }
        connection_list_adapter.notifyDataSetChanged();
    }

    //for debugging only
    /*	private void withrottle_list() {
		try {
			JmDNS jmdns = JmDNS.create();
			ServiceInfo[] infos = jmdns.list("_withrottle._tcp.local.");
			String fh = "";
			for (ServiceInfo info : infos) {
				fh +=  info.getURL() + "\n";
			}
			jmdns.close();
			if (fh != "") {
				Toast.makeText(getApplicationContext(), "Found withrottle servers:\n" + fh, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(), "No withrottle servers found.", Toast.LENGTH_LONG).show();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	 */

    private boolean saveSharedPreferencesToFile() {
        boolean res = false;

        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
        String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue));

        if (prefAutoImportExport.equals(AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT)) {
            if (mainapp.connectedHostName != null) {
                String exportedPreferencesFileName = mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";

                if (!exportedPreferencesFileName.equals(".ed")) {
                    File path = Environment.getExternalStorageDirectory();
                    File engine_driver_dir = new File(path, "engine_driver");
                    engine_driver_dir.mkdir();            // create directory if it doesn't exist

                    res = importExportPreferences.saveSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
                }
            } else {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectUnableToSavePref), Toast.LENGTH_LONG).show();
            }
        } else { // preference is NOT to save the preferences for the host
            res = true;
        }
        return res;
    }

   @SuppressWarnings({ "unchecked" })
   private boolean loadSharedPreferencesFromFile() {
       boolean res = false;
       SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
       String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue)).trim();

       deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
       sharedPreferences.edit().putString("prefAndroidId", deviceId).commit();

       if ((prefAutoImportExport.equals(AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT))
               || (prefAutoImportExport.equals(AUTO_IMPORT_EXPORT_OPTION_CONNECT_ONLY))) {  // automatically load the host specific preferences, if the preference is set
           if (mainapp.connectedHostName != null) {
               String exportedPreferencesFileName = mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
               res = importExportPreferences.loadSharedPreferencesFromFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName, deviceId);
               res = true;
           } else {
               Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectUnableToSavePref), Toast.LENGTH_LONG).show();
           }
       } else { // preference is NOT to load the preferences for the host
           res = true;
       }
       return res;
   }
}
