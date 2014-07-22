package jmri.enginedriver3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * Created by STEVET on 7/8/2014.
 */
public class RetainedTaskFragment extends Fragment {
    int started = 0;

    Thread jmdnsRunnableThread = null;
    ED3Activity activity = null;

    Handler retainedTaskFragmentHandler = new RetainedTaskFragment_Handler();
    Handler jmdnsRunnableHandler;  //this is set by the thread after startup

    public RetainedTaskFragment() {

    }
    @Override
    public void onAttach(Activity activity) {
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onAttach()");
        this.activity = (ED3Activity) activity;  //save ref to the new activity
        super.onAttach(activity);
    }
    @Override
    public void onDetach() {
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onDetach()");
        this.activity = null;  //remove ref to the old activity
        super.onDetach();
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);  //this is what makes the fragment not go away on rotation
    }
    @Override
    public void onStart() {
        started++;
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onStart() " + started);
        super.onStart();
        startThreads();
    }
    @Override
    public void onDestroy() {
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onDestroy()");
        cancelThreads();
        super.onDestroy();
    }
    @Override
    public void onStop() {
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onStop()");
        super.onStop();
    }
    @Override
    public void onResume() {
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onResume()");
        super.onResume();
    }
    @Override
    public void onPause() {
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onPause()");
        super.onPause();
    }
    /** Start the background tasks. */
    public void startThreads() {
        if (jmdnsRunnableThread == null) {
            Log.d(Consts.DEBUG_TAG, "starting the jmdnsRunnableThread");
            jmdnsRunnableThread = new Thread(new JmdnsRunnable(this)); //create thread, pass ref back to this fragment
            jmdnsRunnableThread.start();
        }
    }

    /** Cancel the background tasks TODO: replace with message handler*/
    public void cancelThreads() {
        if (jmdnsRunnableThread != null) {
            Message m = jmdnsRunnableHandler.obtainMessage();
            m.what= MessageType.SHUTDOWN;
            jmdnsRunnableHandler.sendMessage(m);
            jmdnsRunnableThread = null;
        }
    }
    private class RetainedTaskFragment_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageType.SERVICE_RESOLVED:
                    //TODO: Add this server to the "global" list of known servers
                    Message m = activity.ED3ActivityHandler.obtainMessage();
                    m.what = MessageType.SERVER_LIST_CHANGED;
                    activity.ED3ActivityHandler.sendMessage(m);
                    break;
                case MessageType.SERVICE_REMOVED:
                    //TODO: Remove this server from the "global" list of known servers
                    m = activity.ED3ActivityHandler.obtainMessage();
                    m.what = MessageType.SERVER_LIST_CHANGED;
                    activity.ED3ActivityHandler.sendMessage(m);
                    break;
                default:  //don't forward unknown messages
                    Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment_Handler.handleMessage() for unknown");
                    break;
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

}