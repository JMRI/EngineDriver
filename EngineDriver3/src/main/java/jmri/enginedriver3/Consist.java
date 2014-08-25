package jmri.enginedriver3;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * consist -- used as a singleton by each throttle fragment, this keeps track of the throttles
 *   which are assigned to one throttle fragment, provides a number of useful functions needed by
 *   the throttle fragment
 */
public class Consist {
    //throttleKeys of locos attached to this fragment, 0 is lead.  get Throttle from list
    private SparseArray<String> throttlesAttached = new SparseArray<String>();

    private MainApplication mainApp; // hold pointer to mainApp, passed in constructor

    public Consist(MainApplication in_mainApp) {
        mainApp = in_mainApp;
    }

    public void addThrottle(String throttleKey) {
        if (!hasThrottle(throttleKey)) {  //don't add if already attached
            throttlesAttached.put(throttlesAttached.size(), throttleKey);
        }
    }
    @Override
    public String toString() {
        String locoText = "";
        String sep = "";
        for (int i = 0; i < throttlesAttached.size(); i++) {
            locoText += sep + mainApp.getThrottle(throttlesAttached.get(i)).getRosterId();
            sep = "+";
        }
        return locoText;
    }
    public void clear() {
        throttlesAttached.clear();
    }
    public Throttle getLeadThrottle() {
        return mainApp.getThrottle(throttlesAttached.get(0));
    }
    public boolean isEmpty() {
        return (throttlesAttached.size()==0);
    }
    public int getCount() {
        return throttlesAttached.size();
    }
    public boolean hasThrottle(String in_throttleKey) {
        for (int i = 0; i < throttlesAttached.size(); i++) {
            if (throttlesAttached.get(i).equals(in_throttleKey)) {
                return true;
            }
        }
        return false;
    }
    //send the needed requests for each loco in the consist  TODO: adjust based on factor
    public void sendSpeedChange(int in_displayedSpeed) {
        for (int i = 0; i < throttlesAttached.size(); i++) {
            Throttle t = mainApp.getThrottle(throttlesAttached.get(i));
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.SPEED_CHANGE_REQUESTED,
                    t.getThrottleKey(), in_displayedSpeed);
        }
    }
    //send the needed requests for each loco in the consist  TODO: adjust based on direction in consist
    public void sendDirectionChange(int in_direction) {
        for (int i = 0; i < throttlesAttached.size(); i++) {
            Throttle t = mainApp.getThrottle(throttlesAttached.get(i));
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.DIRECTION_CHANGE_REQUESTED,
                    t.getThrottleKey(), in_direction);
        }
    }
    public void saveToPreferences(String prefName) {
        SharedPreferences sharedPreferences = mainApp.getMainActivity().getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        Type t = new TypeToken<SparseArray<String>>(){}.getType();
        //convert the hashmap to json string
        String throttlesAttachedJson = gson.toJson(throttlesAttached, t);
        Log.d(Consts.APP_NAME, "saving as " + throttlesAttachedJson);
        //save the json string as a shared pref
        sharedPreferencesEditor.putString("dynaFragsJson", throttlesAttachedJson);
        sharedPreferencesEditor.commit();
    }
    public void restoreFromPreferences(String prefName) {
        SharedPreferences sharedPreferences = mainApp.getMainActivity().getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        String throttlesAttachedJson = sharedPreferences.getString("dynaFragsJson", "");
        if (!throttlesAttachedJson.equals("")) { //only restore if we found something to restore
            Log.d(Consts.APP_NAME, "restoring dynaFrags from " + throttlesAttachedJson);
            Gson gson = new Gson();
            Type t = new TypeToken<SparseArray<String>>() {}.getType();
            throttlesAttached = gson.fromJson(throttlesAttachedJson, t);  //restore to list
            //verify all throttles are still tracked by app, if not, clear the list
            for (int i = 0; i < throttlesAttached.size(); i++) {
                Throttle throttle = mainApp.getThrottle(throttlesAttached.get(i));
                if (throttle==null) {
                    throttlesAttached.clear();
                    break;
                }
            }
        }
    }

}


