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
    Activity activity = null;

    Handler retainedTaskFragmentHandler = new Handler();
    Handler jmdnsRunnableHandler;

    public RetainedTaskFragment() {

    }
    @Override
    public void onAttach(Activity activity) {
        Log.d(Consts.DEBUG_TAG, "in RetainedTaskFragment.onAttach()");
        this.activity = activity;  //save ref to the new activity
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
            jmdnsRunnableThread = new Thread(new JmdnsRunnable(this)); //pass ref to this fragment
            jmdnsRunnableThread.start();
        }
    }

    /** Cancel the background tasks TODO: replace with message handler*/
    public void cancelThreads() {
        if (jmdnsRunnableThread != null) {
            Message m = jmdnsRunnableHandler.obtainMessage();
            m.what=message_type.SHUTDOWN;
//            m.obj=new String("this is a test message");
//        m.arg1 = msgArg1;
//        m.arg2 = msgArg2;
            jmdnsRunnableHandler.sendMessage(m);
            jmdnsRunnableThread = null;
        }
    }

}