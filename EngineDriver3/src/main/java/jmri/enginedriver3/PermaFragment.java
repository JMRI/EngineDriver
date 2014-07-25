package jmri.enginedriver3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by activity on first run, this fragment has no ui, and will be retained without 
 *   restart until the app is ended.  It is responsible for all of the background threads,
 *   starting, communicating with, and stopping them as needed.  It is also responsible for 
 *   maintaining the shared entities found in mainApp (for now).
 */
public class PermaFragment extends Fragment {

    private static MainApplication mainApp; // hold pointer to mainApp
    private int started = 0;

    private Thread  jmdnsRunnableThread = null;
    public Handler jmdnsRunnableHandler;  //this is set by the thread after startup
    private Thread  debugRunnableThread = null;
    public Handler debugRunnableHandler;  //this is set by the thread after startup
    public MainActivity mainActivity = null;

    Handler permaFragHandler = new PermaFrag_Handler();

    public PermaFragment() {

    }
    @Override
    public void onAttach(Activity activity) {
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onAttach()");
        this.mainActivity = (MainActivity) activity;  //save ref to the new activity
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onCreate()");
        mainApp =(MainApplication)getActivity().getApplication();  //set pointer to app

        //TODO: load from storage, once save is implemented
        //TODO: set base url in connection fragment
//	    setServer("10.10.3.131");
//	    setServer("192.168.1.247");
        mainApp.setServer(null);
//        mainApp.setWiThrottlePort(0);
        mainApp.setWebPort(-1);
        mainApp.discovered_servers_list = new ArrayList<HashMap<String, String> >();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDetach() {
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onDetach()");
        this.mainActivity = null;  //remove ref to the old activity
        super.onDetach();
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);  //this is what makes the fragment not go away on rotation
    }
    @Override
    public void onStart() {
        started++;
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onStart() " + started);
        super.onStart();
        startThreads();
    }
    @Override
    public void onDestroy() {
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onDestroy()");
        cancelThreads();
        super.onDestroy();
    }
    @Override
    public void onStop() {
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onStop()");
        super.onStop();
    }
    @Override
    public void onResume() {
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onResume()");
        super.onResume();
    }
    @Override
    public void onPause() {
        Log.d(Consts.DEBUG_TAG, "in PermaFrag.onPause()");
        super.onPause();
    }
    /** Start the background tasks. */
    public void startThreads() {
        if (jmdnsRunnableThread == null) {
            Log.d(Consts.DEBUG_TAG, "starting the jmdnsRunnableThread");
            jmdnsRunnableThread = new Thread(new JmdnsRunnable(this, mainApp)); //create thread, pass ref back to this fragment
            jmdnsRunnableThread.start();
        }
        if (debugRunnableThread == null) {
            Log.d(Consts.DEBUG_TAG, "starting the debugRunnableThread");
            debugRunnableThread = new Thread(new DebugRunnable(this, mainApp)); //create thread, pass ref back to this fragment
            debugRunnableThread.start();
            mainApp.sendMsg(debugRunnableHandler, MessageType.CONNECTED);  //TODO: this is just a test
        }
    }

    /** Cancel the background tasks via message handler*/
    public void cancelThreads() {
        if (jmdnsRunnableThread != null) {
            Log.d(Consts.DEBUG_TAG, "ending the jmdnsRunnableThread");
            mainApp.sendMsg(jmdnsRunnableHandler, MessageType.SHUTDOWN);
            jmdnsRunnableThread = null;
        }
        if (debugRunnableThread != null) {
            Log.d(Consts.DEBUG_TAG, "ending the debugRunnableThread");
            mainApp.sendMsg(debugRunnableHandler, MessageType.SHUTDOWN);
            debugRunnableThread = null;
        }
    }
    private class PermaFrag_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
//                case MessageType.SERVICE_RESOLVED:
                    //TODO: this is temporary, basically autoconnects to every one found
//                    String li = ds.get("ip_address");
//                    mainApp.setServer(li);
//                    mainApp.setWiThrottlePort(lp);
//                    mainApp.setWebPort(1080);  //TODO: set this after connect, remove hardcoding
//                    mainApp.sendMsg(activity.mainActivityHandler, MessageType.CONNECTED);
//                    break;
                //simply forward these along to activity
                case MessageType.LONG_MESSAGE:
                case MessageType.SHORT_MESSAGE:
                case MessageType.DISCOVERED_SERVER_LIST_CHANGED:
                    if (mainActivity!=null) {
                        mainApp.sendMsg(mainActivity.mainActivityHandler, msg);
                    }
                    break;

                default:  //don't forward unknown messages
                    Log.w(Consts.DEBUG_TAG, "in PermaFrag_Handler.handleMessage() received unknown message type " + msg.what);
                    break;
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

}