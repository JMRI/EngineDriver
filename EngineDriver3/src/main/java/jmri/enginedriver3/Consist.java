package jmri.enginedriver3;

import android.util.SparseArray;

/**
 * consist -- used as a singleton by each throttle fragment, this keeps track of the throttles
 *   which are assigned to one throttle fragment, provides a number of useful functions needed by
 *   the throttle fragment
 */
public class Consist {
    //throttleKeys of locos attached to this fragment, 0 is lead.  get Throttle from list
    private SparseArray<String> throttlesAttached = new SparseArray<String>();

    protected static MainApplication mainApp; // hold pointer to mainApp, passed in constructot

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
}


