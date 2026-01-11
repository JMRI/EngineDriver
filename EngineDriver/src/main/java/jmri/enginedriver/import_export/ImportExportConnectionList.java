package jmri.enginedriver.import_export;

import static jmri.enginedriver.threaded_application.context;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

public class ImportExportConnectionList {
    static final String activityName = "ImportExportConnectionList";

    public ArrayList<HashMap<String, String>> connections_list;
    private final SharedPreferences prefs;
    private final boolean prefHideDemoServer;
    public boolean foundDemoHost = false;

    private static final String demo_host = "jmri.mstevetodd.com";
    private static final String demo_port = "44444";
//    private static final String DUMMY_HOST = "999";
    private static final String DUMMY_ADDRESS = "999";
    private static final int DUMMY_PORT = 999;
//    private static final String DUMMY_SSID = "";

//    private static final int FAILURE_REASON_ERROR_READING = 1;
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
            File connections_list_file = new File(context.getExternalFilesDir(null), "connections_list.txt");

            if (connections_list_file.exists()) {
                BufferedReader list_reader = new BufferedReader(new FileReader(connections_list_file));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    String ip_address;
                    String host_name;
                    String port_str;
                    int port = 0;
                    String ssid_str;
                    String service_type = "";
                    List<String> parts = Arrays.asList(line.split(":", 5)); //split record from file, max of 4 parts
                    if (parts.size() > 1) {  //skip if not split
                        if (parts.size() == 2) {  //old style, use part 1 for ip and host
                            host_name = parts.get(0);
                            ip_address = parts.get(0);
                            port_str = parts.get(1);
                            ssid_str = "";
                        } else if (parts.size() == 3) { //new style
                            host_name = parts.get(0);
                            ip_address = parts.get(1);
                            port_str = parts.get(2);
                            ssid_str = "";
                        } else if (parts.size() == 4) { //new new style, get all 4 parts
                            host_name = parts.get(0);
                            ip_address = parts.get(1);
                            port_str = parts.get(2);
                            ssid_str = parts.get(3);
                        } else { // additional service type format get all 5 parts
                            host_name = parts.get(0);
                            ip_address = parts.get(1);
                            port_str = parts.get(2);
                            ssid_str = parts.get(3);
                            service_type = parts.get(4);
                        }
                        try {  //attempt to convert port to integer
                            port = Integer.decode(port_str);
                        } catch (Exception ignored) {
                        }
                        if (!(ip_address.equals(addressToRemove)) || !(Integer.toString(port).equals(portToRemove))) {
                            if (port > 0) {  //skip if port not converted to integer

                                boolean includeThisHost = true;
                                if (host_name.equals(demo_host) && Integer.toString(port).equals(demo_port)) {
                                    foundDemoHost = true;
                                    if (prefHideDemoServer) includeThisHost = false;
                                }
                                if (includeThisHost) {
                                    HashMap<String, String> hm = new HashMap<>();
                                    hm.put("ip_address", ip_address);
                                    hm.put("host_name", host_name);
                                    hm.put("port", Integer.toString(port));
                                    hm.put("ssid", ssid_str);
                                    hm.put("service_type", service_type);
                                    if (!connections_list.contains(hm)) {    // suppress dups
                                        connections_list.add(hm);
                                    }
                                }
                            }
                        }
                    }
                }
                list_reader.close();
            } else {
                Log.d(threaded_application.applicationName, activityName + ": getConnectionsList(): Recent connections not found");
            }
        } catch (IOException except) {
            errMsg = except.getMessage();
            Log.e(threaded_application.applicationName, activityName + ": getConnectionsList(): Error reading recent connections list: " + errMsg);
            failureReason = errMsg;
//                    threadedApplication.safeToast(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectErrorReadingRecentConnections) + " " + errMsg, Toast.LENGTH_SHORT).show();
        }

        //if demo host not already in list, add it at end
        if ((!prefHideDemoServer) && (!foundDemoHost)) {
            HashMap<String, String> hm = new HashMap<>();
            hm.put("ip_address", demo_host);
            hm.put("host_name", demo_host);
            hm.put("port", demo_port);
            hm.put("ssid", "");
            hm.put("service_type", "_withrottle._tcp.local.");
            connections_list.add(hm);
        }
//        connection_list_adapter.notifyDataSetChanged();
    }

    public void saveConnectionsListExecute(threaded_application myApp, String ip, String name, int port, String wsName, String ssidName, String serviceType) {
        new SaveConnectionsList(myApp, ip, name, port, wsName, ssidName, serviceType);
    }

    class SaveConnectionsList implements Runnable{

        public threaded_application mainapp;  // hold pointer to mainapp

        private final String connected_hostip;
        private final String connected_hostname;
        private final Integer connected_port;
        private final String webServerName;  //name from the webServer
        private final String connected_ssid;
        private final String serviceType;

        public SaveConnectionsList(threaded_application myApp, String ip, String name, int port, String wsName, String ssidName, String sserviceType) {
            mainapp = myApp;
            connected_hostip = ip;
            connected_hostname = name;
            connected_port = port;
            connected_ssid = ssidName;
            webServerName = wsName;
            serviceType = sserviceType;

            new Thread(this).start();
        }


        public void run() {
            String errMsg = "";
            //exit if values not set, avoid NPE reported to Play Store
            if (connected_hostip == null || connected_port == 0)  return;

            foundDemoHost = false;
            boolean isBlankOrDemo = false;
            boolean isDemo = false;
            if (connected_hostname.equals(demo_host) && connected_port.toString().equals(demo_port)) {
                isDemo = true;
                foundDemoHost = true;
            }
            if (((webServerName.isEmpty()) || (connected_hostname.equals(webServerName)))
                    || (isDemo)) {
                isBlankOrDemo = true;
            }

            //if no SD Card present then nothing to do
            if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                return;
            }
            try {
                File connections_list_file = new File(context.getExternalFilesDir(null), "connections_list.txt");
                PrintWriter list_output = new PrintWriter(connections_list_file);

                if (!(connected_hostip.equals(DUMMY_ADDRESS)) || (connected_port != DUMMY_PORT)) {  // will have been called from the remove connection longClick so ignore the current connection values
                    //Write selected connection to file, then write all others (skipping selected if found)
                    if (isBlankOrDemo) {
                        list_output.format("%s:%s:%d:%s:%s\n", connected_hostname, connected_hostip, connected_port, connected_ssid, serviceType);
                    } else {
                        list_output.format("%s:%s:%d:%s:%s\n", webServerName, connected_hostip, connected_port, connected_ssid, serviceType);
                    }
                }

                String smrc = prefs.getString("maximum_recent_connections_preference", ""); //retrieve pref for max recents to show
                if (smrc.isEmpty()) { //if no value or entry removed, set to default
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
                    String sIpAddress = t.get("ip_address");
                    String sHostName = t.get("host_name");
                    String sPort = t.get("port");
                    String sSsid = t.get("ssid");
                    String sType = t.get("service_type");
                    
                    if ( (sIpAddress!=null) &&  (sHostName!=null) &&(sPort!=null) &&  (sSsid!=null) &&  (sType!=null) ) {
                        Integer port = Integer.valueOf(sPort);

                        boolean doWrite = !connected_hostip.equals(sIpAddress) || (connected_port.intValue() != port.intValue());
                        //don't write it out if same as selected
                        if (sIpAddress.equals(demo_host) && port.toString().equals(demo_port) && (foundDemoHost)) {
                            doWrite = false;
                        }
                        if (doWrite) {
                            list_output.format("%s:%s:%d:%s:%s\n", sHostName, sIpAddress, port, sSsid, sType);
                        }
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
                    String connection_log_file_name = "connections_log.txt";

                    File connections_log_file = new File(context.getExternalFilesDir(null), connection_log_file_name);
                    FileWriter fileWriter = new FileWriter(connections_log_file , true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter, 1024);
                    PrintWriter log_output = getPrintWriter(bufferedWriter, isBlankOrDemo, currentDateAndTime);
                    log_output.close();
                    bufferedWriter.close();
                    fileWriter.close();
                } catch (IOException except) {
                    errMsg = except.getMessage();
                }
            }
            if (errMsg != null) displayError(errMsg);
        }

        private PrintWriter getPrintWriter(BufferedWriter bufferedWriter, boolean isBlankOrDemo, String currentDateAndTime) {
            PrintWriter log_output = new PrintWriter(bufferedWriter);

            if (isBlankOrDemo) {
                log_output.format("%s:%s:%d:%s:%s:%s\n", connected_hostname, connected_hostip, connected_port, connected_ssid, serviceType, currentDateAndTime);
            } else {
                log_output.format("%s:%s:%d:%s:%s:%s\n", webServerName, connected_hostip, connected_port, connected_ssid, serviceType, currentDateAndTime);
            }

            log_output.flush();
            return log_output;
        }

        protected void displayError(String errMsg) {
            if (!errMsg.isEmpty())
                threaded_application.safeToast(mainapp.getResources().getString(R.string.toastConnectErrorSavingRecentConnection) + " " + errMsg, Toast.LENGTH_SHORT);
        }
    }
}
