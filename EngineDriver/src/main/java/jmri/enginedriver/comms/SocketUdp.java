package jmri.enginedriver.comms;


import static android.widget.Toast.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jmri.enginedriver.R;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.connection_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.threaded_application;

class SocketUdp extends Thread {
    public static final String activityName = "SocketUdp";
    PrintWriter outputPW = null;

    private DatagramSocket udpSocket;

    boolean socketOk = true;
    private boolean isRunning = true;

    private Thread networkThread;

    private volatile boolean endRead = false;           //signals rcvr to terminate
    private volatile boolean socketGood = false;        //indicates socket condition
    private volatile boolean inboundTimeout = false;    //indicates inbound messages are not arriving from WiT
    private boolean firstConnect = false;               //indicates initial socket connection was achieved
    private int connectTimeoutMs = 3000; //connection timeout in milliseconds
    private int socketTimeoutMs = 500; //socket timeout in milliseconds

    private static final int BUFFER_SIZE = 2048;
    byte[] receiveBuffer = new byte[BUFFER_SIZE];

    private final int MAX_INBOUND_TIMEOUT_RETRIES = 2;
    private int inboundTimeoutRetryCount = 0;           // number of consecutive inbound timeouts
    boolean inboundTimeoutRecovery = false;     // attempting to force WiT to respond

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private final StringBuilder str = new StringBuilder();

    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;
    Context context;

     SocketUdp(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread, Context myContext) {
        super("socketUdp");

        SocketUdp.prefs = prefs;
        SocketUdp.mainapp = mainapp;
        SocketUdp.commThread = commThread;
        context = myContext;
    }

    public boolean connect() {

        threaded_application.logging(activityName+": <:> connect():");

        // Ensure any previous socket is fully closed and null before creating a new one
        if (udpSocket != null) {
            try {
                udpSocket.close();
            } catch (Exception ignored) {}
            udpSocket = null;
        }

        //use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done

        endRead = false;

        connectTimeoutMs = Integer.parseInt(prefs.getString("prefConnectTimeoutMs", mainapp.getResources().getString(R.string.prefConnectTimeoutMsDefaultValue)));
        socketTimeoutMs = Integer.parseInt(prefs.getString("prefSocketTimeoutMs", mainapp.getResources().getString(R.string.prefSocketTimeoutMsDefaultValue)));

        mainapp.connectionType = connection_type.UDP;

        try {
            // Initialize socket (bound to local port)
            udpSocket = new DatagramSocket(null);
            udpSocket.setReuseAddress(true);
            udpSocket.bind(new InetSocketAddress(mainapp.port));
            receiveBuffer = new byte[BUFFER_SIZE];

            socketOk = true;
            socketGood = true;

        } catch (Exception e) {
            socketOk = false;
            socketGood = false;
            threaded_application.logging('e', activityName+": Socket error: ", e);

        }

        //start the SocketUDP thread.
        if (socketOk) {
            if (!this.isAlive()) {
                isRunning = true;
                try {
                    this.start();
                } catch (IllegalThreadStateException except) {
                    //ignore "already started" errors
                    mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorStartingSocket, except.getMessage()), LENGTH_SHORT);
                }
            }
            firstConnect = true;
        }

        threaded_application.logging(activityName+": <:> connect(): socket: " + socketOk);
        return socketOk;
    }

    public void disconnect(boolean shutdown) {
        disconnect(shutdown, false);
    }

    public void disconnect(boolean shutdown, boolean fastShutdown) {
        threaded_application.logging(activityName+": <:> disconnect():");

        socketGood = false;
        isRunning = false;
        comm_thread.heart.stopHeartbeat();
        if (udpSocket != null) {
            udpSocket.close();
            // No need to call disconnect() after close(), close() handles it and releases the port.
            udpSocket = null; // Set to null to ensure it can be re-initialized in connect()
        }

        if (shutdown) {
            endRead = true;
            if (!fastShutdown) {
                for (int i = 0; i < 5 && this.isAlive(); i++) {
                    try {
                        Thread.sleep(200); // Give run() a chance to exit (it will since socket is closed)
                    } catch (InterruptedException e) {
                        mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorSleepingThread, e.getMessage()), Toast.LENGTH_SHORT);
                    }
                }
            }
        }
    }

    public void run() {
        while (isRunning) {
            try {
                if (udpSocket == null || udpSocket.isClosed()) {
                    // If socket is closed or null, exit the loop
                    socketOk = false;
                    socketGood = false;
                    threaded_application.logging('e', activityName + ": Socket closed unexpectedly: ");
                    break;
                }
                // Receive Packet
                threaded_application.logging(activityName + ": <:> Waiting: ");
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(receivePacket); // Blocks until a packet arrives

                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                threaded_application.logging(activityName + ": <:> Received: " + receivedMessage);

                clearInboundTimeout();
                processMessage(receivedMessage);
                // Continuous listen loop
            } catch (Exception e) {
                socketOk = false;
                socketGood = false;
                threaded_application.logging('e', activityName + ": Socket error: ", e);
            }
        }
    }

    private void processMessage(String message) {
        str.append(message);
        while ( (socketGood) && (!endRead) ) {
            try {
                if (str.toString().isEmpty()) break;
                if ( (str.toString().contains("<")) && (str.toString().contains(">")) ) {

                    String wholeStr = str.toString();
                    int endIdx = wholeStr.indexOf(">");
                    String oneStr = wholeStr.substring(wholeStr.indexOf("<"), endIdx + 1);
                    String remainder = (endIdx + 2 <= wholeStr.length()) ? wholeStr.substring(endIdx + 2) : "";

                    threaded_application.logging(activityName+": SocketUdp.read(): whole str »" + wholeStr +"«");
                    threaded_application.logging(activityName+": SocketUdp.read(): one str   »" + oneStr +"«");
                    threaded_application.logging(activityName+": SocketUdp.read(): remainder »" + remainder +"«\n\n");

                    String[] superCmds = oneStr.split("\n");

                    for (int j = 0; j < superCmds.length; j++) {
                        String[] cmds = superCmds[j].split("><");
                        if (cmds.length == 1) { // multiple concatenated commands
                            comm_thread.processWifiResponse(cmds[0]);
                        } else {
                            for (int i = 0; i < cmds.length; i++) {
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
                    str.setLength(0);
                    if (!remainder.isEmpty()) str.append(remainder);

                    comm_thread.heart.restartInboundInterval();
                    clearInboundTimeout();

                    if (remainder.isEmpty()) break;

                } else {
                    threaded_application.logging(activityName+": SocketUdp.read(): partial: »" + str + "«");
                    comm_thread.heart.restartInboundInterval();
                    clearInboundTimeout();
                    break;
                }
            } catch (Exception e) {
                // Handle disconnected or error
                threaded_application.logging(activityName+": SocketUdp.processMessage(): error: " + e.getMessage());
                break;
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    void Send(String msg) {
        boolean reconInProg = false;
        //reconnect socket if needed
        threaded_application.logging(activityName+": SocketUdp.send(): socket: " + (socketGood ? "good" : "bad") + " inboundTimeout: " + (inboundTimeout ? "true" : "false"));
        if ( (!socketGood) || (inboundTimeout)) {
            String status;
            if (mainapp.client_address == null) {
                status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppNotConnected);
                threaded_application.logging(activityName+": SocketUdp.send(): [address null] WiT send reconnection attempt: " + threaded_application.reconnectAttemptCount);
            } else if (inboundTimeout) {
                status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppNoResponse, mainapp.host_ip, Integer.toString(mainapp.port), comm_thread.heart.getInboundInterval());
                threaded_application.logging(activityName+": SocketUdp.send(): [inboundTimeout] WiT receive reconnection attempt: " + threaded_application.reconnectAttemptCount);
            } else {
                status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppUnableToConnect, mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address);
                threaded_application.logging(activityName+": SocketUdp.send(): WiT send reconnection attempt: " + threaded_application.reconnectAttemptCount);
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
            if (udpSocket == null || udpSocket.isClosed()) return;

            threaded_application.reconnectAttemptCount = 0;

            try {
                executor.execute(() -> {
                    try {
                        String message = msg + "\n";
                        byte[] sendData = message.getBytes();
                        InetAddress serverAddress = InetAddress.getByName(mainapp.host_ip);
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, mainapp.port);
                        udpSocket.send(sendPacket);
                        threaded_application.logging(activityName + ": Sent: " + msg);
                    } catch (Exception e) {
                        threaded_application.logging('e', activityName + ": Send error: ", e);
                    }
                });

//                new Thread(() -> {
//                    try {
//                        String message = msg +"\n";
//                        byte[] sendData = message.getBytes();
//                        InetAddress serverAddress = InetAddress.getByName(mainapp.host_ip);
//
//                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, mainapp.port);
//                        udpSocket.send(sendPacket);
//                        threaded_application.logging(activityName+": Sent: " + msg);
//                    } catch (Exception e) {
//                        threaded_application.logging('e', activityName+": Send error: ", e);
//                    }
//                }).start();

                comm_thread.heart.restartOutboundInterval();

                // if we get here without an exception then the socket is ok
                if (reconInProg) {
                    String status = "Connected to WiThrottle Server at " + mainapp.host_ip + ":" + mainapp.port;

                    mainapp.alertCommHandlerWithBundle(message_type.WIT_CON_RECONNECT);

                    threaded_application.logging(activityName+": SocketUdp.send(): WiT reconnection successful.");
                    clearInboundTimeout();
                    comm_thread.heart.restartInboundInterval();     //socket is good so restart inbound heartbeat timer
                }
            } catch (Exception e) {
                threaded_application.logging(activityName+": SocketUdp.send(): WiT xmtr error.");
                socketGood = false;             //output buffer error so force reconnection on next send
            }
        }

        if (!socketGood) {
            mainapp.commBundleMessageHandler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 500L);   //try connection again in 0.5 second
        }
    }

    boolean SocketGood() {
        return this.socketGood;
    }

    void InboundTimeout() {
        if (++inboundTimeoutRetryCount >= MAX_INBOUND_TIMEOUT_RETRIES) {
            threaded_application.logging(activityName+": SocketUdp.InboundTimeout(): WiT max inbound timeouts");
            inboundTimeout = true;
            inboundTimeoutRetryCount = 0;
            inboundTimeoutRecovery = false;
            // force a 'send' to start the reconnection process
            mainapp.commBundleMessageHandler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 200L);
        } else {
            threaded_application.logging(activityName+": SocketUdp.InboundTimeout(): WiT inbound timeout " +
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
