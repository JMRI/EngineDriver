package jmri.enginedriver3;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by stevet on 7/16/2014.
 */
class HeartbeatRunnable implements Runnable {

    private PermaFragment permaFragment;
    private MainApplication mainApp;

    private boolean keepRunning = true;

    //create, expecting refs to permaFrag and mainApp passed in
    public HeartbeatRunnable(PermaFragment in_permaFragment, MainApplication in_mainApp) {
        Log.d(Consts.DEBUG_TAG, "in HeartbeatRunnable()");
        permaFragment = in_permaFragment;
        mainApp = in_mainApp;
    }

    @Override
    public void run() {
        Log.d(Consts.DEBUG_TAG, "starting HeartbeatRunnable.run()");
        Looper.prepare();
        permaFragment.heartbeatRunnableHandler = new Debug_Runnable_Handler();  //update ref to thread's handler back in retained frag
        Looper.loop();  //this runs the message handler, quitting when requested
        Log.d(Consts.DEBUG_TAG, "ending HeartbeatRunnable.run()");
    }

    private class Debug_Runnable_Handler extends Handler {
        //force-start the message loop on this handler
        private Debug_Runnable_Handler() {
            mainApp.sendMsgDelayed(this, mainApp.getJmriHeartbeat() - 1000, MessageType.LOOP);
        }

        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.DEBUG_TAG, "in HeartbeatRunnable.handleMessage()");
            switch (msg.what) {
                case MessageType.SHUTDOWN:
                    this.getLooper().quit(); //stop the looper
                    break;
                case MessageType.LOOP:  //send heartbeat, then kick off repeat, using server heartbeat - 1 second
                    mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.HEARTBEAT);
                    mainApp.sendMsgDelayed(this, mainApp.getJmriHeartbeat() - 1000, MessageType.LOOP);
                    break;
                default:
                    Log.w(Consts.DEBUG_TAG, "in HeartbeatRunnable.handleMessage() received unknown message type " + msg.what);
                    break;
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

}
