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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.connection_type;
import jmri.enginedriver.type.message_type;

class SocketWiFi extends Thread {
    public static final String activityName = "SocketUdp";
    InetAddress host_address;
    Socket clientSocket = null;
    BufferedReader inputBR = null;
    static final int BUFFER_SIZE = 8192;
    PrintWriter outputPW = null;
    private volatile boolean endRead = false;           //signals rcvr to terminate
    private volatile boolean socketGood = false;        //indicates socket condition
    private volatile boolean inboundTimeout = false;    //indicates inbound messages are not arriving from WiT
    private boolean firstConnect = false;               //indicates initial socket connection was achieved
    private int connectTimeoutMs = 3000; //connection timeout in milliseconds
    private int socketTimeoutMs = 500; //socket timeout in milliseconds

    /** @noinspection FieldCanBeLocal*/
    private final int MAX_INBOUND_TIMEOUT_RETRIES = 2;
    private int inboundTimeoutRetryCount = 0;           // number of consecutive inbound timeouts
    boolean inboundTimeoutRecovery = false;     // attempting to force WiT to respond


    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;

    SocketWiFi(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread) {
        super("socketWiFi");

        SocketWiFi.prefs = prefs;
        SocketWiFi.mainapp = mainapp;
        SocketWiFi.commThread = commThread;
    }

    public boolean connect() {

        //use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done
        boolean socketOk = HaveNetworkConnection();

        connectTimeoutMs = Integer.parseInt(prefs.getString("prefConnectTimeoutMs", mainapp.getResources().getString(R.string.prefConnectTimeoutMsDefaultValue)));
        socketTimeoutMs = Integer.parseInt(prefs.getString("prefSocketTimeoutMs", mainapp.getResources().getString(R.string.prefSocketTimeoutMsDefaultValue)));

        mainapp.connectionType = connection_type.TCP;

        //validate address
        if (socketOk) {
            try {
                host_address = InetAddress.getByName(mainapp.host_ip);
            } catch (UnknownHostException except) {
                mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppCantDetermineIp, mainapp.host_ip), LENGTH_SHORT);
                socketOk = false;
            } catch (Exception except) {
                Log.d(threaded_application.applicationName, activityName + ": connect(): Unknown error.");
                socketOk = false;
            }
        }

        //socket
        if (socketOk) {
            try {
                //look for someone to answer on specified socket, and set timeout
                Log.d(threaded_application.applicationName, activityName + ": SocketWiFi: Opening socket, connectTimeout=" + connectTimeoutMs + " and socketTimeout=" + socketTimeoutMs);
                Log.d(threaded_application.applicationName, activityName + ": SocketWiFi: Opening socket, ip=" + mainapp.host_ip + "port=" + mainapp.port);
                clientSocket = new Socket();
                InetSocketAddress sa = new InetSocketAddress(mainapp.host_ip, mainapp.port);
                clientSocket.connect(sa, connectTimeoutMs);
                Log.d(threaded_application.applicationName, activityName + ": SocketWiFi: Opening socket: Connect successful.");
                clientSocket.setSoTimeout(socketTimeoutMs);
                Log.d(threaded_application.applicationName, activityName + ": SocketWiFi: Opening socket: set timeout successful.");
            } catch (Exception except) {
                if (!firstConnect) {
//                        mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppCantConnect,
//                                mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address, except.getMessage()), Toast.LENGTH_LONG);

                    Bundle bundle = new Bundle();
                    String message = mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppCantConnect,
                            mainapp.host_ip,
                            Integer.toString(mainapp.port),
                            mainapp.client_address,
                            except.getMessage());
                    bundle.putString(alert_bundle_tag_type.RESPONSE, message);
                    mainapp.alertActivitiesWithBundle(message_type.CONNECTION_FAILED, bundle, activity_id_type.CONNECTION);

                }
                if ((!mainapp.client_type.equals("WIFI")) && (mainapp.prefAllowMobileData)) { //show additional message if using mobile data
                    Log.d(threaded_application.applicationName, activityName + ": SocketWiFi: Opening socket: Using mobile network, not WIFI. Check your WiFi settings and Preferences.");
                    mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppNotWIFI,
                            mainapp.client_type), Toast.LENGTH_LONG);
                }
                socketOk = false;
            }
        }

        //rcvr
        if (socketOk) {
            try {
                inputBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), BUFFER_SIZE);
            } catch (IOException except) {
                mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorInputStream, except.getMessage()), LENGTH_SHORT);
                socketOk = false;
            }
        }

        //start the SocketWiFi thread.
        if (socketOk) {
            if (!this.isAlive()) {
                endRead = false;
                try {
                    this.start();
                } catch (IllegalThreadStateException except) {
                    //ignore "already started" errors
                    mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorStartingSocket, except.getMessage()), LENGTH_SHORT);
                }
            }
        }

        //xmtr
        if (socketOk) {
            try {
                outputPW = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                if (outputPW.checkError()) {
                    socketOk = false;
                }
            } catch (IOException e) {
                mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorCreatingOutputStream, e.getMessage()), LENGTH_SHORT);
                socketOk = false;
            }
        }
        socketGood = socketOk;
        if (socketOk)
            firstConnect = true;
        return socketOk;
    }

    public void disconnect(boolean shutdown) {
        disconnect(shutdown, false);
    }

    public void disconnect(boolean shutdown, boolean fastShutdown) {
        Log.d(threaded_application.applicationName, activityName + ": SocketWiFi: disconnect()");
        if (shutdown) {
            endRead = true;
            if (!fastShutdown) {
                for (int i = 0; i < 5 && this.isAlive(); i++) {
                    try {
                        Thread.sleep(connectTimeoutMs);     //  give run() a chance to see endRead and exit
                    } catch (InterruptedException e) {
                        mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorSleepingThread, e.getMessage()), LENGTH_SHORT);
                    }
                }
            }
        }

        socketGood = false;

        //close socket
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (Exception e) {
                Log.d(threaded_application.applicationName, activityName + ": SocketWiFi(): Error closing the Socket: " + e.getMessage());
            }
        }
    }

    //read the input buffer
    public void run() {
        String str;
        //continue reading until signaled to exit by endRead
        while (!endRead) {
            if (socketGood) {        //skip read when the socket is down
                try {
                    if ((str = inputBR.readLine()) != null) {
                        if (!str.isEmpty()) {
                            threaded_application.extendedLogging(activityName + ": <<-- " + str);
                            comm_thread.heart.restartInboundInterval();
                            clearInboundTimeout();
                            if (mainapp.isWiThrottleProtocol()) {
                                comm_thread.processWifiResponse(str);
                            } else {
                                String [] cmds = str.split("><");
                                if (cmds.length == 1) { // multiple concatenated commands
                                    comm_thread.processWifiResponse(str);
                                } else {
                                    for (int i=0; i< cmds.length; i++) {
                                        if ((cmds[i].charAt(0) == '<') && (cmds[i].charAt(cmds[i].length() - 1)) == '>') {
                                            comm_thread.processWifiResponse(cmds[i]);
                                        } else if ((cmds[i].charAt(0) == '<') && (cmds[i].charAt(cmds[i].length() - 1)) != '>') {
                                            comm_thread.processWifiResponse(cmds[i] + ">");
                                        } else if ((cmds[i].charAt(0) != '<') && (cmds[i].charAt(cmds[i].length() - 1)) == '>') {
                                            comm_thread.processWifiResponse("<" + cmds[i]);
                                        } else {
                                            comm_thread.processWifiResponse("<" + cmds[i] + ">");
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    socketGood = this.SocketCheck();
                } catch (IOException e) {
                    if (socketGood) {
                        Log.d(threaded_application.applicationName, activityName + ": run(): WiT rcvr error.");
                        socketGood = false;     //input buffer error so force reconnection on next send
                    }
                }
            }
            if (!socketGood) {
                SystemClock.sleep(500L);        //don't become compute bound here when the socket is down
            }
        }
        comm_thread.heart.stopHeartbeat();
        Log.d(threaded_application.applicationName, activityName + ": run(): SocketWiFi exit.");
    }

    @SuppressLint("StringFormatMatches")
    void Send(String msg) {
        boolean reconInProg = false;
        //reconnect socket if needed
        if (!socketGood || inboundTimeout) {
            String status;
            if (mainapp.client_address == null) {
                status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppNotConnected);
                Log.d(threaded_application.applicationName, activityName + ": send(): Not Connected: WiT send reconnection attempt: " + threaded_application.reconnectAttemptCount);
            } else if (inboundTimeout) {
                status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppNoResponse,
                        mainapp.host_ip, Integer.toString(mainapp.port), comm_thread.heart.getInboundInterval(), threaded_application.reconnectAttemptCount);
                Log.d(threaded_application.applicationName, activityName + ": send(): No Response: WiT receive reconnection attempt: " + threaded_application.reconnectAttemptCount);
            } else {
                status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppUnableToConnect, mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address);
                Log.d(threaded_application.applicationName, activityName + ": send(): Unable to connect: WiT send reconnection attempt: " + threaded_application.reconnectAttemptCount);
            }
            socketGood = false;

            threaded_application.reconnectAttemptCount++;

            Bundle witBundle = new Bundle();
            witBundle.putString(alert_bundle_tag_type.MESSAGE, status);
            mainapp.alertActivitiesWithBundle(message_type.WIT_CON_RETRY, witBundle);

            //perform the reconnection sequence
            this.disconnect(false);             //clean up socket but do not shut down the receiver
            this.connect();                     //attempt to reestablish connection
            reconInProg = true;
        }

        //try to send the message
        if (socketGood) {
            threaded_application.reconnectAttemptCount = 0;

            try {
                outputPW.println(msg);
                outputPW.flush();
                comm_thread.heart.restartOutboundInterval();

                // if we get here without an exception then the socket is ok
                if (reconInProg) {
//                        String status = "Connected to WiThrottle Server at " + mainapp.host_ip + ":" + mainapp.port;

                    mainapp.alertCommHandlerWithBundle(message_type.WIT_CON_RECONNECT);

                    Log.d(threaded_application.applicationName, activityName + ": send(): WiT reconnection successful.");
                    clearInboundTimeout();
                    comm_thread.heart.restartInboundInterval();     //socket is good so restart inbound heartbeat timer
                    mainapp.dccexListsRequested = -1; //invalidate the lists
                }
            } catch (Exception e) {
                Log.d(threaded_application.applicationName, activityName + ": send(): WiT xmtr error.");
                socketGood = false;             //output buffer error so force reconnection on next send
            }
        }

        if (!socketGood) {
            mainapp.commBundleMessageHandler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 500L);   //try connection again in 0.5 second
        }
    }

    // Attempt to determine if the socket connection is still good.
    // unfortunately isConnected returns true if the Socket was disconnected other than by calling close()
    // so on signal loss it still returns true.
    // Eventually we just try to send and handle the IOException if the socket was disconnected.
    boolean SocketCheck() {
        boolean status = clientSocket.isConnected() && !clientSocket.isInputShutdown() && !clientSocket.isOutputShutdown();
        if (status)
            status = HaveNetworkConnection();   // can't trust the socket flags so try something else...
        return status;
    }

    // temporary - SocketCheck should determine whether socket connection is good however socket flags sometimes do not get updated
    // so it doesn't work.  This is better than nothing though?
    private boolean HaveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        mainapp.prefAllowMobileData = prefs.getBoolean("prefAllowMobileData", false);

        final ConnectivityManager connectivityManager = (ConnectivityManager) mainapp.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = connectivityManager.getAllNetworkInfo();
        for (NetworkInfo networkInfo : netInfo) {
            if ("WIFI".equalsIgnoreCase(networkInfo.getTypeName()))

                if (!mainapp.prefAllowMobileData) {
                    // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
//                        if ((Build.VERSION.SDK_INT >= 21)
//                                && (!mainapp.haveForcedWiFiConnection)) {
                    if (!mainapp.haveForcedWiFiConnection) {
                        Log.d(threaded_application.applicationName, activityName + ": HaveNetworkConnection(): NetworkRequest.Builder");
                        NetworkRequest.Builder request = new NetworkRequest.Builder();
                        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

                        connectivityManager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {
                            @Override
                            public void onAvailable(@NonNull Network network) {
                                if (Build.VERSION.SDK_INT < 23) {
                                    ConnectivityManager.setProcessDefaultNetwork(network);
                                } else {
                                    connectivityManager.bindProcessToNetwork(network);  //API23+
                                }
                            }
                        });
                        mainapp.haveForcedWiFiConnection = true;
                    }
                }

            if (isNetworkAvailable()) {
                haveConnectedWifi = true;
            } else {
                // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                if (mainapp.prefAllowMobileData) {
                    haveConnectedWifi = true;
                }
            }
            if ("MOBILE".equalsIgnoreCase(networkInfo.getTypeName()))
                if ((isNetworkAvailable()) && (mainapp.prefAllowMobileData)) {
                    haveConnectedMobile = true;
                }
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    private Boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mainapp.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return nwInfo != null && nwInfo.isConnected();
        }
    }

    boolean SocketGood() {
        return this.socketGood;
    }

    void InboundTimeout() {
        if (++inboundTimeoutRetryCount >= MAX_INBOUND_TIMEOUT_RETRIES) {
            Log.d(threaded_application.applicationName, activityName + ": InboundTimeout(): WiT max inbound timeouts");
            inboundTimeout = true;
            inboundTimeoutRetryCount = 0;
            inboundTimeoutRecovery = false;
            // force a 'send' to start the reconnection process
            mainapp.commBundleMessageHandler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 200L);

        } else {
            Log.d(threaded_application.applicationName, activityName + ": InboundTimeout(): WiT inbound timeout " +
                    inboundTimeoutRetryCount + " of " + MAX_INBOUND_TIMEOUT_RETRIES);
            // heartbeat should trigger a WiT reply so force that now
            inboundTimeoutRecovery = true;

            mainapp.commBundleMessageHandler.post(comm_thread.heart.outboundHeartbeatTimer);
        }
    }

    void clearInboundTimeout() {
        inboundTimeout = false;
        inboundTimeoutRecovery = false;
        inboundTimeoutRetryCount = 0;
    }
}
