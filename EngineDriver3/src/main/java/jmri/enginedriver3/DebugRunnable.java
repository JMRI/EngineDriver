package jmri.enginedriver3;

import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

/**
 * Created by stevet on 7/16/2014.
 */
class DebugRunnable implements Runnable {

    private PermaFragment permaFragment;
    private MainApplication mainApp;

    private boolean keepRunning = true;

    //create, expecting refs to permaFrag and mainApp passed in
    public DebugRunnable(PermaFragment in_permaFragment, MainApplication in_mainApp) {
        Log.d(Consts.DEBUG_TAG, "in DebugRunnable()");
        permaFragment = in_permaFragment;
        mainApp = in_mainApp;
    }

    @Override
    public void run() {
        Log.d(Consts.DEBUG_TAG, "starting DebugRunnable.run()");
        Looper.prepare();
        permaFragment.debugRunnableHandler = new Debug_Runnable_Handler();  //update ref to thread's handler back in retained frag
//        Looper.loop();
        while (keepRunning) {
            mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.CONNECTED);
            SystemClock.sleep(10000);
        }
        Log.d(Consts.DEBUG_TAG, "ending DebugRunnable.run()");
    }

    private class Debug_Runnable_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(Consts.DEBUG_TAG, "in DebugRunnable.handleMessage()");
            switch (msg.what) {
                case MessageType.SHUTDOWN:
                    keepRunning = false;
                    break;
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

}
