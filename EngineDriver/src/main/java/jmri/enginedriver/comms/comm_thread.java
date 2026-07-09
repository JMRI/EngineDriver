/* Copyright (C) 2017-2026 M. Steve Todd mstevetodd@gmail.com

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

package jmri.enginedriver.comms;

import static android.widget.Toast.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.net.Inet4Address;
import java.util.LinkedHashMap;
import java.util.Objects;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.connection_type;
import jmri.enginedriver.type.consist_function_rule_style_type;

import jmri.enginedriver.R;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.heartbeat_interval_type;

public class comm_thread extends Thread {
    static final String activityName = "comm_thread";

    JmDNS jmdns = null;
    volatile boolean endingJmdns = false;
    WithrottleListener listener;

    NsdManager nsdManager;
    NsdDiscoveryListener withrottleDiscoveryListener;
    NsdDiscoveryListener jmriDccexDiscoveryListener;
    NsdDiscoveryListener dccexTcpDiscoveryListener;
    NsdDiscoveryListener dccexUdpDiscoveryListener;

    static android.net.wifi.WifiManager.MulticastLock multicast_lock;
    static SocketWiFi socketWiT;
    static SocketUdp socketUdp;
    PhoneListener phone;
    static Heartbeat heart = new Heartbeat();
    private static long lastSentMs = System.currentTimeMillis();
    private static long lastQueuedMs = System.currentTimeMillis();

    protected static threaded_application mainapp;  // hold pointer to mainapp
    protected static SharedPreferences prefs;

    protected String LATCHING_DEFAULT;
    protected String LATCHING_DEFAULT_ENGLISH;

    protected static int requestLocoIdForWhichThrottleDCCEX;

    static final String[] TRACK_TYPES = {"NONE", "MAIN", "MAIN_INV", "PROG", "DC", "DCX", "AUTO", "EXT", "PROG"};
    static final boolean[] TRACK_TYPES_NEED_ID = {false, false, false, false, true, true, false, false, false};

    public comm_thread(threaded_application myApp, SharedPreferences myPrefs) {
        super("comm_thread");

        //noinspection AssignmentToStaticFieldFromInstanceMethod
        mainapp = myApp;
        prefs = myPrefs;

        mainapp.prefUseDccexProtocol = prefs.getString("prefUseDccexProtocol", mainapp.getResources().getString(R.string.prefUseDccexProtocolDefaultValue));
        mainapp.prefAlwaysUseFunctionsFromServer = prefs.getBoolean("prefAlwaysUseFunctionsFromServer", mainapp.getResources().getBoolean(R.bool.prefAlwaysUseFunctionsFromServerDefaultValue));
        LATCHING_DEFAULT = mainapp.getString(R.string.prefFunctionConsistLatchingLightBellDefaultValue); // can change with language
        LATCHING_DEFAULT_ENGLISH = mainapp.getString(R.string.prefFunctionConsistLatchingLightBellDefaultValueEnglish); // can not change with language

        ResponseProcessorDccex.initialise(mainapp, prefs, this);
        ResponseProcessorWiThrottle.initialise(mainapp, prefs, this);
        SendProcessorDccex.initialise(mainapp, prefs, this);
        SendProcessorWiThrottle.initialise(mainapp, prefs, this);

        this.start();
    }

    /* ******************************************************************************************** */

    private final java.util.List<NsdServiceInfo> nsdResolveQueue = new java.util.ArrayList<>();
    private boolean nsdResolveInProgress = false;

    private void resolveNextNsdService() {
        synchronized (nsdResolveQueue) {
            if (nsdResolveInProgress || nsdResolveQueue.isEmpty() || nsdManager == null) {
                return;
            }
            nsdResolveInProgress = true;
            NsdServiceInfo next = nsdResolveQueue.remove(0);
            threaded_application.logging(activityName + ": resolveNextNsdService(): resolving " + next.getServiceName());
            
            nsdManager.resolveService(next, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    threaded_application.logging(activityName + ": resolveNextNsdService: onResolveFailed(): " + serviceInfo.getServiceName() + ", error: " + errorCode);
                    nsdResolveInProgress = false;
                    resolveNextNsdService();
                }

                @Override
                public void onServiceResolved(NsdServiceInfo resolvedServiceInfo) {
                    threaded_application.logging(activityName + ": resolveNextNsdService: onServiceResolved(): " + resolvedServiceInfo.getServiceName());
                    
                    int port = resolvedServiceInfo.getPort();
                    String ip_address = resolvedServiceInfo.getHost().getHostAddress();
                    if (ip_address.startsWith("/")) ip_address = ip_address.substring(1);

                    // Map back to original types for app compatibility
                    String type = resolvedServiceInfo.getServiceType();
                    if (type.endsWith(".")) type = type.substring(0, type.length()-1);
                    String appServiceType = type + ".local.";

                    String host_name = modifyHostName(resolvedServiceInfo.getServiceName(), appServiceType);
                    
                    String serverType = ""; 
                    if (resolvedServiceInfo.getServiceName().toLowerCase().contains("jmri")) serverType = "JMRI";

                    String key = ip_address + ":" + port;
                    mainapp.knownDccexServerIps.put(key, serverType);

                    Bundle bundle = new Bundle();
                    bundle.putString(alert_bundle_tag_type.HOST_NAME, host_name);
                    bundle.putString(alert_bundle_tag_type.IP_ADDRESS, ip_address);
                    bundle.putString(alert_bundle_tag_type.PORT, Integer.toString(port));
                    bundle.putString(alert_bundle_tag_type.SSID, mainapp.client_ssid);
                    bundle.putString(alert_bundle_tag_type.SERVICE_TYPE, appServiceType);
                    mainapp.alertActivitiesWithBundle(message_type.SERVICE_RESOLVED, bundle);

                    nsdResolveInProgress = false;
                    resolveNextNsdService();
                }
            });
        }
    }

    public class NsdDiscoveryListener implements NsdManager.DiscoveryListener {
        private final String originalServiceType;

        public NsdDiscoveryListener(String serviceType) {
            this.originalServiceType = serviceType;
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            threaded_application.logging(activityName + ": NsdDiscoveryListener: onStartDiscoveryFailed(): " + serviceType + ", error: " + errorCode);
            stopDiscovery();
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            threaded_application.logging(activityName + ": NsdDiscoveryListener: onStopDiscoveryFailed(): " + serviceType + ", error: " + errorCode);
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            threaded_application.logging(activityName + ": NsdDiscoveryListener: onDiscoveryStarted(): " + serviceType);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            threaded_application.logging(activityName + ": NsdDiscoveryListener: onDiscoveryStopped(): " + serviceType);
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            threaded_application.logging(activityName + ": NsdDiscoveryListener: onServiceFound(): " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType());
            synchronized (nsdResolveQueue) {
                nsdResolveQueue.add(serviceInfo);
                resolveNextNsdService();
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            threaded_application.logging(activityName + ": NsdDiscoveryListener: onServiceLost(): " + serviceInfo.getServiceName());
            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.SERVICE, serviceInfo.getServiceName());
            mainapp.alertActivitiesWithBundle(message_type.SERVICE_REMOVED, bundle);
        }

        private void stopDiscovery() {
            threaded_application.logging(activityName + ": NsdDiscoveryListener: stopDiscovery()");
            try {
                nsdManager.stopServiceDiscovery(this);
            } catch (Exception ignored) {}
        }
    }

    /* ******************************************************************************************** */

    //Listen for a WiThrottle service advertisement on the LAN.
    public class WithrottleListener implements ServiceListener {

        public void serviceAdded(ServiceEvent event) {
            //          threaded_application.logging(activityName + ": serviceAdded()");
            //A service has been added. If no details, ask for them
            threaded_application.logging(activityName + ": " + String.format("serviceAdded(): for '%s', Type='%s'", event.getName(), event.getType()));
            ServiceInfo si = jmdns.getServiceInfo(event.getType(), event.getName(), 0);
            if (si == null || si.getPort() == 0) {
                threaded_application.logging(activityName + ": " + String.format("serviceAdded(): requesting details: '%s', Type='%s'", event.getName(), event.getType()));
                jmdns.requestServiceInfo(event.getType(), event.getName(), true, 1000);
            }
        }

        public void serviceRemoved(ServiceEvent event) {
            //Tell the UI thread to remove from the list of services available.
            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.SERVICE, event.getName());
            mainapp.alertActivitiesWithBundle(message_type.SERVICE_REMOVED, bundle);
            threaded_application.logging(activityName + ": " + String.format("serviceRemoved(): '%s'", event.getName()));
        }

        public void serviceResolved(ServiceEvent event) {
            //          threaded_application.logging(activityName + ": " + String.format("serviceResolved()"));
            //A service's information has been resolved. Send the port and service name to connect to that service.
            int port = event.getInfo().getPort();

            String serverType = event.getInfo().getPropertyString("jmri") == null ? "" : "JMRI";

            String host_name = modifyHostName(event.getInfo().getName(), event.getInfo().getType());
            Inet4Address[] ip_addresses = event.getInfo().getInet4Addresses();  //only get ipV4 address
            String ip_address = ip_addresses[0].toString().substring(1);  //use first one, since WiThrottle is only putting one in (for now), and remove leading slash

            String key = ip_address + ":" + port;
            mainapp.knownDccexServerIps.put(key, serverType);

            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.HOST_NAME, host_name);
            bundle.putString(alert_bundle_tag_type.IP_ADDRESS, ip_address);
            bundle.putString(alert_bundle_tag_type.PORT, ((Integer) port).toString());
            bundle.putString(alert_bundle_tag_type.SSID, mainapp.client_ssid);
            bundle.putString(alert_bundle_tag_type.SERVICE_TYPE, event.getInfo().getType());
            mainapp.alertActivitiesWithBundle(message_type.SERVICE_RESOLVED, bundle);

            threaded_application.logging(activityName + ": " + String.format("serviceResolved(): %s(%s):%d -- %s",
                    host_name, ip_address, port,
                    event.toString().replace(Objects.requireNonNull(System.getProperty("line.separator")), " ")));

        }
    }

    String modifyHostName(String hostName, String serviceType) {
        String tempServiceType = (serviceType.charAt(0)!='.') ? serviceType : serviceType.substring(1);
        String resultHostName;
        resultHostName = switch (tempServiceType) {
            case threaded_application.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP,
                 threaded_application.JMRI_TYPE ->
                    hostName + " [JMRI DCC-EX]";
            case threaded_application.JMDNS_SERVICE_DCC_EX_TCP,
                 threaded_application.DCCEX_TCP_TYPE ->
                    hostName + " [TCP DCC-EX]";
            case threaded_application.JMDNS_SERVICE_DCC_EX_UDP,
                 threaded_application.DCCEX_UDP_TYPE ->
                    hostName + " [UDP DCC-EX]";
            default ->
                    hostName;
        };
        return resultHostName;
    }

    void startJmdns() {
        threaded_application.logging(activityName + ": startJmdns()");
        if ( (mainapp.appIsFinishing) || (mainapp.exitConfirmed) ) {
            threaded_application.logging(activityName + ": startJmdns(): shutting down. do nothing");
            return;
        }


        //Set up to find a WiThrottle service via ZeroConf
        try {
            if (mainapp.client_address != null) {

                if (endingJmdns) {
                    threaded_application.logging(activityName + ": startJmdns(): waiting for previous JmDNS to finish closing...");
                    for (int i = 0; i < 100 && endingJmdns; i++) { // wait up to 10 seconds
                        //noinspection BusyWait
                        Thread.sleep(100);
                    }
                    if (endingJmdns) {
                        threaded_application.logging(activityName + ": startJmdns(): WARNING: previous JmDNS still closing, proceeding anyway");
                    }
                    Thread.sleep(500); // Give the OS/Network stack extra time to release the port
                }

                WifiManager wifi = (WifiManager) mainapp.getSystemService(Context.WIFI_SERVICE);

                if (multicast_lock == null) {
                    multicast_lock = wifi.createMulticastLock("engine_driver");
                    multicast_lock.setReferenceCounted(false);
                }

                if (!multicast_lock.isHeld()) {
                    multicast_lock.acquire();
                }

                threaded_application.logging(activityName + ": startJmdns(): local IP addr " + mainapp.client_address);

                // pass ip as address to bind to specifically
                // On Android 7, some devices have issues if port 5353 is already bound by the system.
                // JmDNS 3.6.x tries to use SO_REUSEADDR, but it may still fail if not supported or blocked.
                
                int attempts = 0;
                while ( (jmdns == null) && (attempts < 3) && (!mainapp.appIsFinishing) && (!mainapp.exitConfirmed) )  {
                    attempts++;
                    try {
                        String jmdnsName = "EngineDriver-" + System.currentTimeMillis();
                        if (attempts == 1) {
                            threaded_application.logging(activityName + ": startJmdns(): attempt 1: JmDNS.create(" + mainapp.client_address + ", " + jmdnsName + ")");
                            // Use unique name to help avoid some conflicts on older Android
                            jmdns = JmDNS.create(mainapp.client_address_inet4, jmdnsName);
                        } else if (attempts == 2) {
                            threaded_application.logging(activityName + ": startJmdns(): attempt 2: JmDNS.create(null, " + jmdnsName + ")");
                            jmdns = JmDNS.create(null, jmdnsName);
                        } else {
                            threaded_application.logging(activityName + ": startJmdns(): attempt 3: JmDNS.create()");
                            Thread.sleep(500);
                            jmdns = JmDNS.create();
                        }
                    } catch (Exception e) {
                        threaded_application.logging(activityName + ": startJmdns(): attempt " + attempts + " failed: " + e.getMessage());
                        String errMsg = (e.getMessage() != null) ? e.getMessage().toUpperCase() : "";
                        if (errMsg.contains("EADDRINUSE") || errMsg.contains("ADDRESS ALREADY IN USE")) {
                            if (attempts < 3) {
                                threaded_application.logging(activityName + ": startJmdns(): port in use, will retry...");
                                Thread.sleep(200);
                            }
                        } else {
                            throw e; // unknown error
                        }
                    }
                }

                if (jmdns != null) {
                    listener = new WithrottleListener();
                    threaded_application.logging(activityName + ": startJmdns(): listener created");

                    jmdns.addServiceListener(threaded_application.JMDNS_SERVICE_WITHROTTLE, listener);
                    jmdns.addServiceListener(threaded_application.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP, listener);
                    jmdns.addServiceListener(threaded_application.JMDNS_SERVICE_DCC_EX_TCP, listener);
                    jmdns.addServiceListener(threaded_application.JMDNS_SERVICE_DCC_EX_UDP, listener);
                    threaded_application.logging(activityName + ": startJmdns(): jmdns listeners added");
                    
                    // Trigger a query for all services immediately in a separate thread as list() is blocking
                    new Thread(() -> {
                        try {
                            if (jmdns != null) {
                                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): query JMDNS_SERVICE_WITHROTTLE");
                                jmdns.list(threaded_application.JMDNS_SERVICE_WITHROTTLE);
                            }
                        } catch (Exception ignored) {}
                    }, "JmDNS-Query-withrottle").start();

                    new Thread(() -> {
                        try {
                            if (jmdns != null) {
                                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): query JMDNS_SERVICE_JMRI_DCCPP_OVERTCP");
                                jmdns.list(threaded_application.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP);
                            }
                        } catch (Exception ignored) {}
                    }, "JmDNS-Query-JMRI-TCP").start();

                    new Thread(() -> {
                        try {
                            if (jmdns != null) {
                                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): query JMDNS_SERVICE_DCC_EX_TCP");
                                jmdns.list(threaded_application.JMDNS_SERVICE_DCC_EX_TCP);
                            }
                        } catch (Exception ignored) {}
                    }, "JmDNS-Query-DCC-EX-TCP").start();

                    new Thread(() -> {
                        try {
                            if (jmdns != null) {
                                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): query JMDNS_SERVICE_DCC_EX_UDP");
                                jmdns.list(threaded_application.JMDNS_SERVICE_DCC_EX_UDP);
                            }
                        } catch (Exception ignored) {}
                    }, "JmDNS-Query-DCC-EX-UDP").start();

//                    new Thread(() -> {
//                        try {
//                            if (jmdns != null) {
//                                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): query JMDNS_SERVICE_WITHROTTLE");
//                                jmdns.list(threaded_application.JMDNS_SERVICE_WITHROTTLE);
//                                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): query JMDNS_SERVICE_JMRI_DCCPP_OVERTCP");
//                                jmdns.list(threaded_application.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP);
//                                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): query JMDNS_SERVICE_DCC_EX_TCP");
//                                jmdns.list(threaded_application.JMDNS_SERVICE_DCC_EX_TCP);
//                                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): query JMDNS_SERVICE_DCC_EX_UDP");
//                                jmdns.list(threaded_application.JMDNS_SERVICE_DCC_EX_UDP);
//                            }
//                        } catch (Exception ignored) {}
//                    }, "JmDNS-Query").start();
                }
                
                // On many Android 5-9 devices, JmDNS fails to bind port 5353.
                // We always try NsdManager as a fallback (or in parallel if JmDNS starts but finds nothing)
                // For now, if JmDNS failed OR we are on a problematic version, start NsdManager.
                if (jmdns == null || Build.VERSION.SDK_INT <= 28) {
                    threaded_application.logging(activityName + ": startJmdns(): JmDNS failed or SDK<=25, starting NsdManager fallback/parallel");
                    startNsdFallback();
                }

            } else {
                mainapp.safeToast(R.string.toastThreadedAppNoLocalIp, Toast.LENGTH_LONG);
            }
        } catch (Exception except) {
            threaded_application.logging('e', activityName + ": startJmdns(): Error creating withrottle listener: " + except.getMessage());
            mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorCreatingWiThrottle, except.getMessage()), LENGTH_SHORT);
            // don't release lock here if we might be retrying later, but since we are exiting startJmdns...
            if (multicast_lock != null && multicast_lock.isHeld() && jmdns == null && !endingJmdns) {
                multicast_lock.release();
            }
        }

        threaded_application.logging(activityName + ": startJmdns(): end.");
    }

    void startNsdFallback() {
        threaded_application.logging(activityName + ": startNsdFallback()");
        if ( (mainapp.appIsFinishing) || (mainapp.exitConfirmed) ) {
            threaded_application.logging(activityName + ": startNsdFallback(): shutting down. do nothing");
            return;
        }
        if (nsdManager == null) {
            nsdManager = (NsdManager) mainapp.getSystemService(Context.NSD_SERVICE);
        }

        withrottleDiscoveryListener = new NsdDiscoveryListener(threaded_application.JMDNS_SERVICE_WITHROTTLE);
        jmriDccexDiscoveryListener = new NsdDiscoveryListener(threaded_application.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP);
        dccexTcpDiscoveryListener = new NsdDiscoveryListener(threaded_application.JMDNS_SERVICE_DCC_EX_TCP);
        dccexUdpDiscoveryListener = new NsdDiscoveryListener(threaded_application.JMDNS_SERVICE_DCC_EX_UDP);

        try {
            threaded_application.logging(activityName + ": startNsdFallback(): starting NsdManager discovery for WiThrottle, JMRI and DCC-EX...");
            nsdManager.discoverServices(threaded_application.WT_TYPE, NsdManager.PROTOCOL_DNS_SD, withrottleDiscoveryListener);
            nsdManager.discoverServices(threaded_application.JMRI_TYPE, NsdManager.PROTOCOL_DNS_SD, jmriDccexDiscoveryListener);
            nsdManager.discoverServices(threaded_application.DCCEX_TCP_TYPE, NsdManager.PROTOCOL_DNS_SD, dccexTcpDiscoveryListener);
            nsdManager.discoverServices(threaded_application.DCCEX_UDP_TYPE, NsdManager.PROTOCOL_DNS_SD, dccexUdpDiscoveryListener);
        } catch (Exception e) {
            threaded_application.logging(activityName + ": startNsdFallback(): Exception starting NsdManager: " + e.getMessage());
        }
    }

    void stopNsd() {
        threaded_application.logging(activityName + ": stopNsd()");

        if (nsdManager != null) {
            try {
                if (withrottleDiscoveryListener != null) nsdManager.stopServiceDiscovery(withrottleDiscoveryListener);
                if (jmriDccexDiscoveryListener != null) nsdManager.stopServiceDiscovery(jmriDccexDiscoveryListener);
                if (dccexTcpDiscoveryListener != null) nsdManager.stopServiceDiscovery(dccexTcpDiscoveryListener);
                if (dccexUdpDiscoveryListener != null) nsdManager.stopServiceDiscovery(dccexUdpDiscoveryListener);
            } catch (Exception e) {
                threaded_application.logging(activityName + ": stopNsd(): Exception: " + e.getMessage());
            }
            withrottleDiscoveryListener = null;
            jmriDccexDiscoveryListener = null;
            dccexTcpDiscoveryListener = null;
            dccexUdpDiscoveryListener = null;
            nsdManager = null;
        }

        threaded_application.logging(activityName + ": stopNsd(): end");
    }

    //endJmdns() takes a long time, so put it in its own thread
    void endJmdns() {
        threaded_application.logging(activityName + ": endJmdns()");

        stopNsd();

        if (!jmdnsIsActive()) {      //only need to run one instance of this thread to terminate jmdns
            threaded_application.logging(activityName + ": endJmdns(): not active");
            return;
        }

        final JmDNS localJmdns = jmdns;
        final WithrottleListener localListener = listener;

        jmdns = null; // Set to null immediately so a new one can be started if needed
        listener = null;
        endingJmdns = true;

        Thread jmdnsThread = new Thread("EndJmdns") {
            @Override
            public void run() {
                try {
                    threaded_application.logging(activityName + ": endJmdns(): unregistering all services and removing listeners");
                    localJmdns.unregisterAllServices();
                    localJmdns.removeServiceListener(threaded_application.JMDNS_SERVICE_WITHROTTLE, localListener);
                    localJmdns.removeServiceListener(threaded_application.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP, localListener);
                    localJmdns.removeServiceListener(threaded_application.JMDNS_SERVICE_DCC_EX_TCP, localListener);
                    localJmdns.removeServiceListener(threaded_application.JMDNS_SERVICE_DCC_EX_UDP, localListener);

                } catch (Exception e) {
                    threaded_application.logging(activityName + ": endJmdns(): exception in jmdns unregister/removeListener: " + e.getMessage());
                }
                try {
                    threaded_application.logging(activityName + ": endJmdns(): calling jmdns.close()");
                    localJmdns.close();
                    threaded_application.logging(activityName + ": endJmdns(): after jmdns.close()");
                } catch (Exception e) {
                    threaded_application.logging(activityName + ": endJmdns(): exception in jmdns.close()");
                } finally {
                    if (multicast_lock != null && multicast_lock.isHeld()) {
                        multicast_lock.release();
                    }
                    endingJmdns = false;
                }
                threaded_application.logging(activityName + ": endJmdns(): run exit");
            }
        };
        jmdnsThread.start();
        threaded_application.logging(activityName + ": endJmdns(): active so ending it and starting thread to remove listener");
    }

    boolean jmdnsIsActive() {
        return jmdns != null;
    }

    /*
      add configuration of digitrax LnWi or DCCEX to discovered list, since they do not provide mDNS
     */
    void addFakeDiscoveredServer(String entryName, String clientAddr, String entryPort, String serverType) {

        if (clientAddr == null || clientAddr.lastIndexOf(".") < 0)
            return; //bail on unexpected value

        //assume that the server is at x.y.z.1
        String server_addr = clientAddr.substring(0, clientAddr.lastIndexOf("."));
        server_addr += ".1";

//        mainapp.knownDccexServerIps.put(server_addr, serverType);
        String key = server_addr+":"+entryPort;
        mainapp.knownDccexServerIps.put(key, serverType);

        Bundle bundle = new Bundle();
        bundle.putString(alert_bundle_tag_type.HOST_NAME, entryName);
        bundle.putString(alert_bundle_tag_type.IP_ADDRESS, server_addr);
        bundle.putString(alert_bundle_tag_type.PORT, entryPort);
        bundle.putString(alert_bundle_tag_type.SSID, mainapp.client_ssid);
        bundle.putString(alert_bundle_tag_type.SERVICE_TYPE, (serverType.equals("DCC-EX") ? threaded_application.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP : threaded_application.JMDNS_SERVICE_WITHROTTLE) );
        mainapp.alertActivitiesWithBundle(message_type.SERVICE_RESOLVED, bundle);

        threaded_application.logging(activityName + ": " + String.format("addFakeDiscoveredServer(): added '%s' at %s to Discovered List", entryName, server_addr));

    }

    protected void stoppingConnection() {
        threaded_application.logging(activityName + ": stoppingConnection(): ");
        heart.stopHeartbeat();
        if (phone != null) {
            phone.disable();
            phone = null;
        }
        endJmdns();
//            dlMetadataTask.stop();
        threaded_application.dlRosterTask.stop();
    }

    protected void delayedAction(int action, long delay) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (action) {
                    case message_type.SHUTDOWN: {
                        shutdown(false);
                        break;
                    }
//                    case message_type.WIFI_QUIT: {
//                        sendQuit();
//                        break;
//                    }
                    case message_type.DISCONNECT: {
                        sendDisconnect();
                        break;
                    }
                }

            }
        }, delay);
    }

    protected void shutdown(boolean fast) {
        threaded_application.logging(activityName + ": Shutdown()");

        if (mainapp.connectionType == connection_type.TCP) {
            if (socketWiT != null) {
                socketWiT.disconnect(true, fast);     //stop reading from the socket
            }
        } else {
            if (socketUdp != null) {
                socketUdp.disconnect(true, fast);     //stop reading from the socket
            }
        }
        threaded_application.logging(activityName + ": Shutdown(): socketWit down");
        mainapp.writeSharedPreferencesToFileIfAllowed();
        mainapp.host_ip = null;
        mainapp.port = 0;
        threaded_application.reinitStatics();                    // ensure activities are ready for relaunch
        mainapp.doFinish = false;                   //ok for activities to run if restarted after this

        threaded_application.dlRosterTask.stop();
//            dlMetadataTask.stop();

        // make sure flashlight is switched off at shutdown
        if (threaded_application.flashlight != null) {
            threaded_application.flashlight.setFlashlightOff();
            threaded_application.flashlight.teardown();
        }
        mainapp.flashState = false;
        threaded_application.logging(activityName + ": Shutdown(): end");
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    protected void sendThrottleName() {
        sendThrottleName(true);
    }
    private static void sendThrottleName(Boolean sendHWID) {
        if (mainapp.isWiThrottleProtocol()) { // Withrottle
            SendProcessorWiThrottle.sendThrottleName(sendHWID);
        } else { //DCC-EX
            SendProcessorDccex.sendThrottleName(sendHWID);
        }
    }

    static void sendAcquireLoco(String addr, String rosterName, int whichThrottle) {
        if (mainapp.isWiThrottleProtocol()) { // Withrottle
            SendProcessorWiThrottle.sendAcquireLoco(addr, rosterName, whichThrottle);
        } else { //DCC-EX
            SendProcessorDccex.sendAcquireLoco(addr, rosterName, whichThrottle);
        }
    }

    protected void sendReleaseLoco(String addr, int whichThrottle)  {
        if (mainapp.isWiThrottleProtocol()) { // Withrottle
            SendProcessorWiThrottle.sendReleaseLoco(addr, whichThrottle);
        }  // else  // DCC-EX has no equivalent
    }

    protected void sendReacquireAllConsists() {
        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++)
            sendReacquireConsist(mainapp.consists[i], i);
    }

    protected void sendReacquireConsist(Consist c, int whichThrottle) {
        for (Consist.ConLoco l : c.getLocos()) { // reacquire each confirmed loco in the consist
            if (l.isConfirmed()) {
                String addr = l.getAddress();
                String roster_name = l.getRosterName();
                sendAcquireLoco(addr, roster_name, whichThrottle); //ask for next loco, with 0 or more delays
            }
        }
    }

    protected void sendEStop(int whichThrottle) {
        if (mainapp.isWiThrottleProtocol()) { // Withrottle
            SendProcessorWiThrottle.sendEStop(whichThrottle);
        } else { //DCC-EX
            SendProcessorDccex.sendEStop(whichThrottle);
        }
    }

    protected void sendEStopOneThrottle(int whichThrottle) {
        sendSpeed(whichThrottle, -1); // -1 = EStop
    }

    protected void sendDisconnect() {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendDisconnect();
        } else { //  DCC-EX
            SendProcessorDccex.sendDisconnect();
        }
    }

// Keep these here in case we find a need for them later
//    protected void sendFunction(char cWhichThrottle, String addr, int fn, int fState) {
//        sendFunction(mainapp.throttleCharToInt(cWhichThrottle), addr, fn, fState, false);
//    }
//    protected void sendFunction(char cWhichThrottle, String addr, int fn, int fState, boolean force) {
//        sendFunction(mainapp.throttleCharToInt(cWhichThrottle), addr, fn, fState);
//    }
//    protected void sendFunction(int whichThrottle, String addr, int fn, int fState) {
//        sendFunction(whichThrottle, addr, fn, fState, false);
//    }
    protected void sendFunction(int whichThrottle, String addr, int fn, int fState, boolean force) {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendFunction( whichThrottle, addr, fn, fState, force);
        } else { //  DCC-EX
            SendProcessorDccex.sendFunction( whichThrottle, addr, fn, fState, force);
        }
    }

    protected void sendTurnout(String systemName, char action) {
        if (mainapp.isWiThrottleProtocol()) { // WiThrottle
            SendProcessorWiThrottle.sendTurnout(systemName, action);
        } else { //DCC-EX
            SendProcessorDccex.sendTurnout(systemName, action);
        }
    }

    protected void sendRoute(String systemName, char action) {
        if (mainapp.isWiThrottleProtocol()) { // WiThrottle
            SendProcessorWiThrottle.sendRoute(systemName, action);
        } else { //DCC-EX
            SendProcessorDccex.sendRoute(systemName, action);
        }
    }

    // WiThrottle and DCC-EX
    @SuppressLint("DefaultLocale")
    protected void sendPower(int powerState) {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendPower(powerState);
        } else { //DCC-EX
            SendProcessorDccex.sendPower(powerState);
        }
    }

    protected void sendQuit() {
        if (mainapp.isWiThrottleProtocol()) { // WiThrottle
            SendProcessorWiThrottle.sendQuit();
        } /// N/A for DCC-EX
    }

    protected void sendHeartbeatStart() {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendHeartbeatStart();
        } else { //DCC-EX
            SendProcessorDccex.sendHeartbeatStart();
        }
    }

    protected void sendDirection(int whichThrottle, String addr, int dir) {
    if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
        SendProcessorWiThrottle.sendDirection(whichThrottle, addr, dir);
    } else { //DCC-EX
        SendProcessorDccex.sendDirection(whichThrottle, addr, dir);
    }
}

    protected static void sendSpeedZero(int whichThrottle) {
        sendSpeed(whichThrottle, 0);
    }

    // WiThrottle and DCC-EX
    @SuppressLint("DefaultLocale")
    protected static void sendSpeed(int whichThrottle, int speed) {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendSpeed(whichThrottle, speed);
        } else { //DCC-EX
            SendProcessorDccex.sendSpeed(whichThrottle, speed);
        }
    }

//    @SuppressLint("DefaultLocale")
//    private void sendRequestDir(int whichThrottle) {
//        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
//            wifiSend(String.format("M%sA*<;>qR",
//                    mainapp.throttleIntToString(whichThrottle)));
//
//        } else { //DCC-EX
//            Consist con = mainapp.consists[whichThrottle];
//            String msgTxt = "";
//            for (Consist.ConLoco l : con.getLocos()) {
//                msgTxt = String.format("<t %s>", l.getAddress().substring(1,l.getAddress().length()));
//                wifiSend(msgTxt);
    ////                threaded_application.logging(activityName + ": sendRequestDir(): DCC-EX: " + msgTxt);
//            }
//        }
//    }

    @SuppressLint("DefaultLocale")
    protected static void sendRequestSpeedAndDir(int whichThrottle) {
        if (mainapp.isWiThrottleProtocol()) { // WiThrottle
            SendProcessorWiThrottle.sendRequestSpeedAndDir(whichThrottle);
        } else { //DCC-EX
            SendProcessorDccex.sendRequestSpeedAndDir(whichThrottle);
        }
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    protected static void processWifiResponse(String responseStr) {
        /* Withrottle Protocol
            See java/arc/jmri/jmrit/withrottle/deviceserver.java for server code and some documentation
            Also see https://www.jmri.org/help/en/package/jmri/jmrit/withrottle/Protocol.shtml for documentation on the protocol
           DCC-EX Native Protocol
            See https://dcc-ex.com/reference/software/command-summary-consolidated.html#gsc.tab=0 for documentation on the protocol
        */

        //send response to debug log for review
        threaded_application.logging(activityName + ": processWifiResponse(): " + (mainapp.isDccexProtocol() ? "DCC-EX" : "      ") + " :<>: <-- :" + responseStr);

        if (mainapp.activityBundleMessageHandlers[activity_id_type.RECONNECT_STATUS] != null) {
            // The reconnect screen must be active, so notify it so that it can be killed, then process the response as normal
            mainapp.alertActivitiesWithBundle(message_type.WIT_CON_RECONNECT, activity_id_type.RECONNECT_STATUS);
        }

        boolean skipDefaultAlertToAllActivities = false;          //set to true if the Activities do not need to be Alerted

        if (mainapp.isWiThrottleProtocol()) { // WiThrottle Protocol. not DCC-EX Native Protocol
            skipDefaultAlertToAllActivities = ResponseProcessorWiThrottle.processWifiResponse(responseStr);
        } else { // DCC-EX
            skipDefaultAlertToAllActivities = ResponseProcessorDccex.processWifiResponse(responseStr);
        }

        if (!skipDefaultAlertToAllActivities) { // if it has not been processed...
            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.COMMAND, responseStr);
            mainapp.alertActivitiesWithBundle(message_type.RESPONSE, bundle);  //send response to running activities

            threaded_application.logging(activityName + ": processWifiResponse(): Unable to process command: " + responseStr);
        }
    }  //end of processWifiResponse

    /* ***********************************  *********************************** */
    /*  Common/Shared processing functions */
    /* ***********************************  *********************************** */

        static boolean acceptMessageOrAlert(String incomingMessage) {
        boolean acceptMessage = true;
        String[] messagesToIgnore;
        if (mainapp.isDccexProtocol()) {
            messagesToIgnore = mainapp.getResources().getStringArray(R.array.dccex_alert_messages_to_ignore);
        } else {
            messagesToIgnore = mainapp.getResources().getStringArray(R.array.withrottle_alert_messages_to_ignore);
        }
        for (String message : messagesToIgnore) {
            if (incomingMessage.equals(message)) {
                acceptMessage = false;
                break;
            }
        }
        return acceptMessage;
    }

    /* ***********************************  *********************************** */

    static void processRosterFunctionString(String responseStr, int whichThrottle) {
        threaded_application.logging(activityName + ": processRosterFunctionString(): processing function labels for " + mainapp.throttleIntToString(whichThrottle));
        LinkedHashMap<Integer, String> functionLabelsMap = threaded_application.parseFunctionLabels(responseStr);
        mainapp.function_labels[whichThrottle] = functionLabelsMap; //set the appropriate global variable from the temp
    }

    /* ***********************************  *********************************** */

    //parse function state string into appropriate app variable array
    static void processFunctionState(int whichThrottle, Integer fn, boolean fState) {

        boolean skip = (fn > 2)
                && (mainapp.prefAlwaysUseDefaultFunctionLabels)
                && (!mainapp.prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.ORIGINAL));

        if (!skip) {
            try {
                mainapp.function_states[whichThrottle][fn] = fState;
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
        }
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    //
    // wifiSend(String msg)
    //
    //send formatted msg to the socket
    //  intermessage gap enforced by requeueing messages as needed
    protected static void wifiSend(String msg) {
        threaded_application.extendedLogging(activityName + ": wifiSend(): message: '" + msg + "'");
        if (msg == null) { //exit if no message
            threaded_application.logging(activityName + ": comm_thread.wifiSend: --> null msg");
            return;
        } else {
            if (mainapp.connectionType == connection_type.TCP) {
                if (socketWiT == null) {
                    threaded_application.logging('e', activityName + ": comm_thread.wifiSend: socketWiT is null, message '" + msg + "' not sent!");
                    return;
                }
            } else {
                if (socketUdp == null) {
                    threaded_application.logging('e', activityName + ": comm_thread.wifiSend: socketUdp is null, message '" + msg + "' not sent!");
                    return;
                }
            }
        }

        long now = System.currentTimeMillis();
        long lastGap = now - lastSentMs;

        //send if sufficient gap between messages or msg is timingSensitive, requeue if not
        if (lastGap >= threaded_application.wifi_send_interval || timingSensitive(msg)) {
            //perform the 'send'
            //noinspection UnnecessaryUnicodeEscape
            threaded_application.logging(activityName + ": wifiSend(): " + (mainapp.isDccexProtocol() ? "DCC-EX" : "      ") + "            :<>: -->: " + msg.replaceAll("\n", "\u21B5") + " (" + lastGap + ")"); //replace newline with cr arrow
            lastSentMs = now;
            if (mainapp.connectionType == connection_type.TCP) {
                socketWiT.Send(msg);
            } else {
                socketUdp.Send(msg);
            }

            if (threaded_application.dccexScreenIsOpen) { // only relevant to some DCC-EX commands that we want to see in the DCC-EC Screen.
                Bundle bundle = new Bundle();
                bundle.putString(alert_bundle_tag_type.COMMAND, msg);
                mainapp.alertActivitiesWithBundle(message_type.DCCEX_COMMAND_ECHO, bundle);
            }
        } else {
            //requeue this message
            int nextGap = Math.max((int) (lastQueuedMs - now), 0) + (threaded_application.wifi_send_interval + 5); //extra 5 for processing
            //noinspection UnnecessaryUnicodeEscape
            threaded_application.logging(activityName + ": wifiSend(): requeue:" + msg.replaceAll("\n", "\u21B5") +
                    ", lastGap=" + lastGap + ", nextGap=" + nextGap); //replace newline with cr arrow

            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.MESSAGE, msg);
            mainapp.alertCommHandlerWithBundle(message_type.WIFI_SEND, nextGap, bundle);

            lastQueuedMs = now + nextGap;
        }
    }  //end wifiSend()

    /* true indicates that message should NOT be requeued as the timing of this message
         is critical.
     */
    private static boolean timingSensitive(String msg) {
        boolean ret = false;
        if (mainapp.isWiThrottleProtocol()) {
            if (msg.matches("^M[0-5]A.{1,5}<;>F[0-1][\\d]{1,2}$")) {
                ret = true;
            } //any function key message
        }
        if (ret) threaded_application.logging(activityName + ": timingSensitive(): timeSensitive msg, not requeuing:");
        return ret;
    }

    public void run() {
        Looper.prepare();
        Looper threadLooper = Looper.myLooper();
        mainapp.commBundleMessageHandler = new comm_handler(threadLooper);
        mainapp.commBundleMessageHandler.initialise(mainapp, prefs, this);
        Looper.loop();
        threaded_application.logging(activityName + ": run() exit");
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    /* ******************************************************************************************** */

    static class Heartbeat {
        //  outboundHeartbeat - send a periodic heartbeat to WiT to show that ED is alive.
        //  inboundHeartbeat - WiT doesn't send a heartbeat to ED, so send a periodic message to WiT that requires a response.
        //
        //  If the HeartbeatValueFromServer is 0 then set heartbeatOutboundInterval = DEFAULT_OUTBOUND_HEARTBEAT_INTERVAL,
        //    and set heartbeatInboundInterval = 0, to disable the inbound heartbeat checks
        //
        //  Otherwise, set heartbeatOutboundInterval to HeartbeatValueFromServer * HEARTBEAT_RESPONSE_ALLOWANCE,
        //    and set heartbeatInboundInterval to HeartbeatValueFromServer / HEARTBEAT_RESPONSE_ALLOWANCE
        //
        //  Insure both values are between MIN_OUTBOUND_HEARTBEAT_INTERVAL and MAX_OUTBOUND_HEARTBEAT_INTERVAL

        private int heartbeatIntervalSetpoint = 0;      //WiT heartbeat interval in msec
        private int heartbeatOutboundInterval = 0;      //sends outbound heartbeat message at this rate (msec)
        private int heartbeatInboundInterval = 0;       //alerts user if there was no inbound traffic for this long (msec)

        public boolean isHeartbeatSent() {
            return heartbeatSent;
        }

        public void setHeartbeatSent(boolean heartbeatSent) {
            this.heartbeatSent = heartbeatSent;
        }

        private boolean heartbeatSent = false;

        int getInboundInterval() {
            return heartbeatInboundInterval;
        }

        /***
         * startHeartbeat(timeoutInterval in milliseconds)
         * calculate the inbound and outbound intervals and starts the beating
         *
         * @param timeoutInterval the WiT timeoutInterval in milliseconds
         */
        void startHeartbeat(int timeoutInterval) {
            //update interval timers only when the heartbeat timeout interval changed
            mainapp.prefHeartbeatResponseFactor = threaded_application.getIntPrefValue(prefs, "prefHeartbeatResponseFactor", mainapp.getApplicationContext().getResources().getString(R.string.prefHeartbeatResponseFactorDefaultValue));

            if (timeoutInterval != heartbeatIntervalSetpoint) {
                heartbeatIntervalSetpoint = timeoutInterval;

                // outbound interval (in ms)
                int outInterval;
                if (heartbeatIntervalSetpoint == 0) {   //wit heartbeat is disabled so use default outbound heartbeat
                    outInterval = heartbeat_interval_type.DEFAULT_OUTBOUND;
                } else {
//                        outInterval = (int) (heartbeatIntervalSetpoint * HEARTBEAT_RESPONSE_FACTOR);
                    outInterval = (int) (heartbeatIntervalSetpoint * ( (double) mainapp.prefHeartbeatResponseFactor) / 100);
                    //keep values in a reasonable range
                    if (outInterval < heartbeat_interval_type.MIN_OUTBOUND)
                        outInterval = heartbeat_interval_type.MIN_OUTBOUND;
                    if (outInterval > heartbeat_interval_type.MAX_OUTBOUND)
                        outInterval = heartbeat_interval_type.MAX_OUTBOUND;
                }
                heartbeatOutboundInterval = outInterval;

                // inbound interval
                int inInterval = mainapp.heartbeatInterval;
                if (heartbeatIntervalSetpoint == 0) {    // wit heartbeat is disabled so disable inbound heartbeat
                    inInterval = 0;
                } else {
                    if (inInterval < heartbeat_interval_type.MIN_INBOUND)
                        inInterval = heartbeat_interval_type.MIN_INBOUND;
                    if (inInterval < outInterval)
//                            inInterval = (int) (outInterval / HEARTBEAT_RESPONSE_FACTOR);
                        inInterval = (int) (outInterval / ( ((double) mainapp.prefHeartbeatResponseFactor) / 100) );
                    if (inInterval > heartbeat_interval_type.MAX_INBOUND)
                        inInterval = heartbeat_interval_type.MAX_INBOUND;
                }
                heartbeatInboundInterval = inInterval;
                //sInboundInterval = Integer.toString(inInterval);    // seconds

                restartOutboundInterval();
                restartInboundInterval();
            }
        }

        //restartOutboundInterval()
        //restarts the outbound interval timing - call this after sending anything to WiT that requires a response
        void restartOutboundInterval() {
            mainapp.commBundleMessageHandler.removeCallbacks(outboundHeartbeatTimer);                   //remove any pending requests
            if (heartbeatOutboundInterval > 0) {
                mainapp.commBundleMessageHandler.postDelayed(outboundHeartbeatTimer, heartbeatOutboundInterval);    //restart interval
            }
        }

        //restartInboundInterval()
        //restarts the inbound interval timing - call this after receiving anything from WiT
        void restartInboundInterval() {
            mainapp.commBundleMessageHandler.removeCallbacks(inboundHeartbeatTimer);
            if (heartbeatInboundInterval > 0) {
                mainapp.commBundleMessageHandler.postDelayed(inboundHeartbeatTimer, heartbeatInboundInterval);
            }
        }

        void stopHeartbeat() {
            mainapp.commBundleMessageHandler.removeCallbacks(outboundHeartbeatTimer);           //remove any pending requests
            mainapp.commBundleMessageHandler.removeCallbacks(inboundHeartbeatTimer);
            heartbeatIntervalSetpoint = 0;
            threaded_application.logging(activityName + ": stopHeartbeat(): heartbeat stopped.");
        }

        //outboundHeartbeatTimer()
        //sends a periodic message to WiT
        final Runnable outboundHeartbeatTimer = new Runnable() {
            @Override
            public void run() {
                mainapp.commBundleMessageHandler.removeCallbacks(this);             //remove pending requests
                if (heartbeatIntervalSetpoint != 0) {
                    boolean anySent = false;
                    if (mainapp.isWiThrottleProtocol()) {
                        for (int i = 0; i < mainapp.prefNumThrottles; i++) {
                            if (mainapp.consists[i].isActive()) {
                                sendRequestSpeedAndDir(i);
                                anySent = true;
                            }
                        }
                    }
                    // prior to JMRI 4.20 there were cases where WiT might not respond to
                    // speed and direction request.  If inboundTimeout handling is in progress
                    // then we always send the Throttle Name to ensure a response
                    if (mainapp.connectionType == connection_type.TCP) {
                        if (!anySent || (mainapp.getServerType().isEmpty() && socketWiT.inboundTimeoutRecovery)) {
                            sendThrottleName(false);    //send message that will get a response
                        }
                    } else {
                        if (!anySent || (mainapp.getServerType().isEmpty() && socketUdp.inboundTimeoutRecovery)) {
                            sendThrottleName(false);    //send message that will get a response
                        }
                    }
                    mainapp.commBundleMessageHandler.postDelayed(this, heartbeatOutboundInterval);   //set next beat
                }
            }
        };

        //inboundHeartbeatTimer()
        //display an alert message when there is no inbound traffic from WiT within required interval
        private final Runnable inboundHeartbeatTimer = new Runnable() {
            @Override
            public void run() {
                mainapp.commBundleMessageHandler.removeCallbacks(this); //remove pending requests
                if (heartbeatIntervalSetpoint != 0) {
                    if (mainapp.connectionType == connection_type.TCP) {
                        if (socketWiT != null && socketWiT.SocketGood()) {
                            socketWiT.InboundTimeout();
                        }
                    } else {
                        if (socketUdp != null && socketUdp.SocketGood()) {
                            socketUdp.InboundTimeout();
                        }
                    }
                    mainapp.commBundleMessageHandler.postDelayed(this, heartbeatInboundInterval);    //set next inbound timeout
                }
            }
        };
    }

    /* ******************************************************************************************** */

    static class PhoneListener extends PhoneStateListener {
        private final TelephonyManager telMgr;

        PhoneListener() {
            telMgr = (TelephonyManager) mainapp.getSystemService(Context.TELEPHONY_SERVICE);
            this.enable();
        }

        public void disable() {
            telMgr.listen(this, PhoneStateListener.LISTEN_NONE);
        }

        public void enable() {
            try {
                telMgr.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
            } catch (SecurityException e) {
                threaded_application.logging('e', activityName + ": PhoneListener(): enable(): SecurityException encountered (and ignored) for telMgr");
            }
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                if (prefs.getBoolean("prefStopOnPhoneCall",
                        mainapp.getResources().getBoolean(R.bool.prefStopOnPhoneCallDefaultValue))) {
                    threaded_application.logging(activityName + ": onCallStateChanged(): Phone is OffHook, Stopping Trains");
                    for (int i = 0; i < mainapp.prefNumThrottles; i++) {
                        if (mainapp.consists[i].isActive()) {
                            sendSpeedZero(i);
                        }
                    }
                }
            }
        }
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

}

