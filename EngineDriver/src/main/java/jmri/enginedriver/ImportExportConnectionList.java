package jmri.enginedriver;

import static jmri.enginedriver.threaded_application.context;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

class ImportExportConnectionList {
    public ArrayList<HashMap<String, String>> connections_list;
    private SharedPreferences prefs;
    private boolean prefHideDemoServer = false;
    public boolean foundDemoHost = false;

    private static final String demo_host = "jmri.mstevetodd.com";
    private static final String demo_port = "44444";
    private static final String DUMMY_HOST = "999";
    private static final String DUMMY_ADDRESS = "999";
    private static final int DUMMY_PORT = 999;

    private static final int FAILURE_REASON_ERROR_READING = 1;
    public String failureReason = "";

    public ImportExportConnectionList(SharedPreferences p) {
        prefs = p;
        prefHideDemoServer = prefs.getBoolean("prefHideDemoServer", context.getResources().getBoolean(R.bool.prefHideDemoServerDefaultValue));
        connections_list = new ArrayList<>();
    }
    public void getConnectionsList(String addressToRemove, String portToRemove) {
        connections_list.clear();
        String errMsg;

        try {
//            File sdcard_path = Environment.getExternalStorageDirectory();
//            File connections_list_file = new File(sdcard_path, "engine_driver/connections_list.txt");
            File connections_list_file = new File(context.getExternalFilesDir(null), "connections_list.txt");

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
                        if (!(ip_address.equals(addressToRemove)) || !(port.toString().equals(portToRemove))) {
                            if (port > 0) {  //skip if port not converted to integer

                                if (!prefHideDemoServer || !(host_name.equals(demo_host) && port.toString().equals(demo_port))) {
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
                }
                list_reader.close();
            } else {
                Log.d("connection_activity", "Recent connections not found");
            }
        } catch (IOException except) {
            errMsg = except.getMessage();
            Log.e("connection_activity", "Error reading recent connections list: " + errMsg);
            failureReason = errMsg;
//                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectErrorReadingRecentConnections) + " " + errMsg, Toast.LENGTH_SHORT).show();
        }

        //if demo host not already in list, add it at end
        if ((!prefHideDemoServer) && (!foundDemoHost)) {
            HashMap<String, String> hm = new HashMap<>();
            hm.put("ip_address", demo_host);
            hm.put("host_name", demo_host);
            hm.put("port", demo_port);
            connections_list.add(hm);
        }
//        connection_list_adapter.notifyDataSetChanged();
    }

    public void saveConnectionsListExecute(threaded_application myApp, String ip, String name, int port, String wsName) {
        new saveConnectionsList(myApp, ip, name, port, wsName).execute();
    }

    class saveConnectionsList extends AsyncTask<Void, Void, String> {

        public threaded_application mainapp;  // hold pointer to mainapp

        private String connected_hostip;
        private String connected_hostname;
        private Integer connected_port;
        private String webServerName;  //name from the webServer

        public saveConnectionsList(threaded_application myApp, String ip, String name, int port, String wsName) {
            mainapp = myApp;
            connected_hostip = ip;
            connected_hostname = name;
            connected_port = port;
            webServerName = wsName;
        }

        @Override
        protected String doInBackground(Void... params) {

            String errMsg = "";
            //exit if values not set, avoid NPE reported to Play Store
            if (connected_hostip == null || connected_port == 0) return errMsg;

            foundDemoHost = false;

            //if no SD Card present then nothing to do
            if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
                return errMsg;
            try {
//                File path = Environment.getExternalStorageDirectory();
//                File engine_driver_dir = new File(path, "engine_driver");
//                //noinspection ResultOfMethodCallIgnored
//                engine_driver_dir.mkdir();            // create directory if it doesn't exist
//
//                File connections_list_file = new File(path, "engine_driver/connections_list.txt");
                File connections_list_file = new File(context.getExternalFilesDir(null), "connections_list.txt");
                PrintWriter list_output = new PrintWriter(connections_list_file);

                if (!(connected_hostip.equals(DUMMY_ADDRESS)) || (connected_port != DUMMY_PORT)) {  // will have been called from the remove connection longClick so ignore the current connection values
                    //Write selected connection to file, then write all others (skipping selected if found)
                    if ( ((webServerName.equals("")) || (connected_hostname.equals(webServerName)))
                        || (connected_hostname.equals(demo_host) && connected_port.toString().equals(demo_port)) ) {
                        list_output.format("%s:%s:%d\n", connected_hostname, connected_hostip, connected_port);
                    } else {
                        list_output.format("%s:%s:%d\n", webServerName, connected_hostip, connected_port);
                    }
                }
                if (connected_hostname.equals(demo_host) && connected_port.toString().equals(demo_port)) {
                    foundDemoHost = true;
                }

                String smrc = prefs.getString("maximum_recent_connections_preference", ""); //retrieve pref for max recents to show
                if (smrc.equals("")) { //if no value or entry removed, set to default
                    smrc = mainapp.getApplicationContext().getResources().getString(R.string.prefMaximumRecentConnectionsDefaultValue);
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

                    boolean doWrite = true;
                    if (connected_hostip.equals(li) && (connected_port.intValue() == lp.intValue())){  //dont write it out if same as selected
                        doWrite = false;
                    }
                    if ( li.equals(demo_host) && lp.toString().equals(demo_port) && (foundDemoHost) ) {
                        doWrite = false;
                    }
                    if (doWrite) {
                        list_output.format("%s:%s:%d\n", lh, li, lp);
                    }
                }
                list_output.flush();
                list_output.close();
            } catch (IOException except) {
                errMsg = except.getMessage();
            }

            if (mainapp.logged_host_ip == null) {
                mainapp.logged_host_ip = connected_hostip;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                    String currentDateAndTime = sdf.format(new Date());

//                    File path = Environment.getExternalStorageDirectory();
//                    File engine_driver_dir = new File(path, "engine_driver");
//                    //noinspection ResultOfMethodCallIgnored
//                    engine_driver_dir.mkdir();            // create directory if it doesn't exist
//
//                    String connection_log_file_name = "engine_driver/connections_log.txt";
                    String connection_log_file_name = "connections_log.txt";
//
//                    PrintWriter log_output = new PrintWriter(new FileWriter(path + "/" + connection_log_file_name, true));
                    File connections_log_file = new File(context.getExternalFilesDir(null), connection_log_file_name);
                    PrintWriter log_output = new PrintWriter(connections_log_file);

                    if (((webServerName.equals("")) || (connected_hostname.equals(webServerName)))
                            || (connected_hostname.equals(demo_host) && connected_port.toString().equals(demo_port))) {
                        log_output.printf("%s:%s:%d:%s\n", connected_hostname, connected_hostip, connected_port, currentDateAndTime);
                    } else {
                        log_output.printf("%s:%s:%d:%s\n", webServerName, connected_hostip, connected_port, currentDateAndTime);
                    }

                    log_output.close();
                } catch (IOException except) {
                    errMsg = except.getMessage();
                }
            }

            return errMsg;
        }

        @Override
        protected void onPostExecute(String errMsg) {
            if (errMsg.length() > 0)
                Toast.makeText(mainapp, mainapp.getResources().getString(R.string.toastConnectErrorSavingRecentConnection) + " " + errMsg, Toast.LENGTH_SHORT).show();
        }
    }
}
