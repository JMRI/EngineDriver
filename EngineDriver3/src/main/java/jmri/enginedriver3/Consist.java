package jmri.enginedriver3;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * consist -- used as a singleton by each throttle fragment, this keeps track of the throttles
 *   which are assigned to one throttle fragment, provides a number of useful functions needed by
 *   the throttle fragment
 */
public class Consist {
  //throttleKeys of locos attached to this fragment, 0 is lead.  get Throttle from list
  private ArrayList<String> throttlesAttached = new ArrayList<String>();

  private ArrayList<String> functionNames = new ArrayList<String>();
  private ArrayList<String> functionLabels = new ArrayList<String>();
  private ArrayList<Boolean> functionLockables = new ArrayList<Boolean>();
  private ArrayList<Button> functionButtons = new ArrayList<Button>();

  private MainApplication mainApp; // hold pointer to mainApp, passed in constructor

  public Consist(MainApplication in_mainApp) {
    mainApp = in_mainApp;
  }

  public void addThrottle(String throttleKey) {
    if (!hasThrottle(throttleKey)) {  //don't add if already attached
      throttlesAttached.add(throttleKey);
      if (throttlesAttached.size() == 1) {  //if just added lead, copy functions to consist
        functionNames.clear();
        functionLabels.clear();
        functionLockables.clear();
        functionButtons.clear();
        Throttle throttle = getLeadThrottle();
        RosterEntry re = mainApp.getRosterEntry(throttle.getRosterId());
        if (re != null && re.getFunctionCount()>0) {  //show defined function keys
          for (int k = 0; k < re.getFunctionCount(); k++) {
            Log.d(Consts.APP_NAME, "copying "+re.getFunctionName(k)+" to "+ k);
            functionNames.add(k, re.getFunctionName(k));
            functionLabels.add(k, re.getFunctionLabel(k));
            functionLockables.add(k, re.getFunctionLockable(k));
          }

        } else { //otherwise, show default function keys, since no rosterentry found
          //TODO: allow user to set this
          functionNames.add(0, "F0");
          functionLabels.add(0, "Light");
          functionLockables.add(0, true);
          functionNames.add(1, "F1");
          functionLabels.add(1, "Horn");
          functionLockables.add(1, false);
        }
      }
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
  public String getFunctionName(int fnId) {
    return functionNames.get(fnId);
  }
  public String getFunctionLabel(int fnId) {
    return functionLabels.get(fnId);
  }
  public Button getFunctionButton(int fnId) {
//    Log.d(Consts.APP_NAME, "requesting fnId=" + fnId);
    if (fnId < 0 || fnId > functionButtons.size()) {
      return null;
    }
    return functionButtons.get(fnId);
  }
  public void setFunctionButton(int fnId, Button button) {
    functionButtons.add(fnId, button);
  }
  public Boolean getFunctionLockable(int fnId) {
    return functionLockables.get(fnId);
  }
  public int getFunctionCount() {
    return functionNames.size();
  }
  public boolean isEmpty() {
    return (throttlesAttached.size()==0);
  }
  public int getThrottleCount() {
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
  //send the needed requests for each loco in the consist  TODO: decide which locos to send it to
  public void sendFunctionChange(String fnName, int newState) {
    for (int i = 0; i < throttlesAttached.size(); i++) {
      Throttle t = mainApp.getThrottle(throttlesAttached.get(i));
      String js = t.getFunctionChangeJson(fnName, newState);
      mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.SEND_JSON_MESSAGE, js);
    }
  }
  public void saveToPreferences(String prefName) {
    SharedPreferences sharedPreferences = mainApp.getMainActivity().getSharedPreferences(prefName,
        Context.MODE_PRIVATE);
    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
    Gson gson = new Gson();
    Type t = new TypeToken<ArrayList<String>>(){}.getType();
    String throttlesAttachedJson = gson.toJson(throttlesAttached, t);
    Log.d(Consts.APP_NAME, "saving throttles as " + throttlesAttachedJson);
    sharedPreferencesEditor.putString("throttlesAttachedJson", throttlesAttachedJson);
    String functionNamesJson = gson.toJson(functionNames, t);
    Log.d(Consts.APP_NAME, "saving functionNames as " + functionNamesJson);
    sharedPreferencesEditor.putString("functionNamesJson", functionNamesJson);
    String functionLabelsJson = gson.toJson(functionLabels, t);
    Log.d(Consts.APP_NAME, "saving functionLabels as " + functionLabelsJson);
    sharedPreferencesEditor.putString("functionLabelsJson", functionLabelsJson);
    String functionLockablesJson = gson.toJson(functionLockables, t);
    Log.d(Consts.APP_NAME, "saving functionLockables as " + functionLockablesJson);
    sharedPreferencesEditor.putString("functionLockablesJson", functionLockablesJson);
    sharedPreferencesEditor.commit();
  }
  public void restoreFromPreferences(String prefName) {
    functionButtons.clear();  //just clear, object references should not be restored

    throttlesAttached.clear();  //clear these in case issues below
    functionNames.clear();
    functionLabels.clear();
    functionLockables.clear();
    SharedPreferences sharedPreferences = mainApp.getMainActivity().getSharedPreferences(prefName,
        Context.MODE_PRIVATE);
    String throttlesAttachedJson = sharedPreferences.getString("throttlesAttachedJson", "");
    if (!throttlesAttachedJson.equals("")) { //only restore if we found something to restore
      Log.d(Consts.APP_NAME, "restoring consist from " + throttlesAttachedJson);
      Gson gson = new Gson();
      Type t = new TypeToken<ArrayList<String>>() {}.getType();
      try {
        throttlesAttached = gson.fromJson(throttlesAttachedJson, t);  //restore to list
      } catch (Exception e) {
        return;  //any issue don't restore
      }
      //verify all throttles are still tracked by app, if not, clear the list
      for (int i = 0; i < throttlesAttached.size(); i++) {
        Throttle throttle = mainApp.getThrottle(throttlesAttached.get(i));
        if (throttle==null) {
          throttlesAttached.clear();
          return;
        }
      }
      String functionNamesJson = sharedPreferences.getString("functionNamesJson", "");
      if (!functionNamesJson.equals("")) { //only restore if we found something to restore
        try {
          functionNames = gson.fromJson(functionNamesJson, t);  //restore to list
        } catch (Exception e) {  //ignore issues with this restore
        }
      }
      String functionLabelsJson = sharedPreferences.getString("functionLabelsJson", "");
      if (!functionLabelsJson.equals("")) { //only restore if we found something to restore
        try {
          functionLabels = gson.fromJson(functionLabelsJson, t);  //restore to list
        } catch (Exception e) {  //ignore issues with this restore
        }
      }
      String functionLockablesJson = sharedPreferences.getString("functionLockablesJson", "");
      if (!functionLockablesJson.equals("")) { //only restore if we found something to restore
        try {
          functionLockables = gson.fromJson(functionLockablesJson, t);  //restore to list
        } catch (Exception e) {  //ignore issues with this restore
        }
      }
    }
  }

}


