package jmri.enginedriver3;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * throttle -- a single address on a single fragment
 *   same address may be on multiple fragments, and multiple addresses on a single fragment
 *   this object corresponds to a throttle as defined by the jmri json server
 *   they will be stored in a list keyed by the unique (to jmri) throttleId
 *   this object is designed to hold updates from the server, regardless of how they are to be
 *   shown on various fragments
 */
public class Throttle {
  private String _throttleKey;  //unique name as known to jmri
//  private String _fragmentName;  //name of the fragment this item belongs to
  private ArrayList<String> _fragmentNames = new ArrayList<String>();  //list of fragments this throttle is part of
  private String _rosterId;  //if not from a roster, set to dccAddress
  private int _dccAddress;
  private float _speed;  //jmri speed, goes from 0.0 to 1.0
  private boolean _forward; //true if current dcc direction is normal, false if opposite
  private boolean _confirmed; //true after jmri has accepted this loco
  private int _speedUnits;  //how many speed intervals between stop and full (8, 10, 14, 28, 126)
  private HashMap<String, Boolean> _fKeyState = new HashMap<String, Boolean>();

  public Throttle(String _throttleKey, String _fragmentName, String _rosterId) {
    this._throttleKey = _throttleKey;
    this._fragmentNames.add(0, _fragmentName); //add as first fragment
    this._rosterId = _rosterId;
    this._dccAddress = 0;
    this._speed = (float) -1.0;
    this._forward = true;
    this._confirmed = false;  //very important, this will be set only after the server responds with this loco
    this._speedUnits = 100;  //TODO: get this from settings or decoder
  }

  public Throttle(String _throttleKey, String _fragmentName, String _rosterId, int _dccAddress, float _speed,
                  boolean _forward, boolean _confirmed, int _speedUnits) {
    this._throttleKey = _throttleKey;
    this._fragmentNames.add(0, _fragmentName); //add as first fragment
    this._rosterId = _rosterId;
    this._dccAddress = _dccAddress;
    this._speed = _speed;
    this._forward = _forward;
    this._confirmed = _confirmed;
    this._speedUnits = _speedUnits;
  }

  public String getThrottleKey() {
    return _throttleKey;
  }

  public void setThrottleKey(String _throttleKey) {
    this._throttleKey = _throttleKey;
  }

  public ArrayList<String> getFragmentNames() {
    return _fragmentNames;
  }

  public void setFragmentNames(ArrayList<String> _fragmentNames) {
    this._fragmentNames = _fragmentNames;
  }

  public String getRosterId() {
    return _rosterId;
  }

  public void setRosterId(String _rosterId) {
    this._rosterId = _rosterId;
  }

  public int getDccAddress() {
    return _dccAddress;
  }

  public void setDccAddress(int _dccAddress) {
    this._dccAddress = _dccAddress;
  }

  public float getSpeed() {
    return _speed;
  }
  public void setSpeed(float _speed) {
    this._speed = _speed;
  }  //TODO: add flag for actually changed
  public int getDisplayedSpeed() {
    return (int) ((_speed * _speedUnits) + 0.5);  //convert based on units selected, round up
  }
  public int getMaxDisplayedSpeed() { return _speedUnits; }
  public int getDisplayedSpeedIncrement() { return (int)((_speedUnits/10.0)+0.5); }  //10% for now, todo: set this in prefs
  public String getSpeedUnitsText() { return ((_speedUnits==100) ? "%" : String.valueOf(_speedUnits)); }

  public boolean isForward() {
    return _forward;
  }

  public void setForward(boolean _forward) {
    this._forward = _forward;
  }

  public boolean isConfirmed() {
    return _confirmed;
  }

  public void setConfirmed(boolean _confirmed) {
    this._confirmed = _confirmed;
  }

  public int getSpeedUnits() {return _speedUnits;}
  public void setSpeedUnits(int _speedUnits) {this._speedUnits = _speedUnits;}


  public float getSpeedForDisplayedSpeed(int displayedSpeed) {
    return ((float)displayedSpeed / (float)_speedUnits);
  }

  public String getSpeedChangeJson(float newJmriSpeed) {
//        {"type":"throttle","data":{"throttle":"CSX754","speed":0.25}}
    String s = "{\"type\":\"throttle\",\"data\":{\"throttle\":\"" + _throttleKey + "\"";
    s += ",\"speed\":" + newJmriSpeed;
    s += "}}";
    return s;
  }
  public String getDirectionChangeJson(int newDirection) {
    String s = "{\"type\":\"throttle\",\"data\":{\"throttle\":\"" + _throttleKey + "\"";
    s += ",\"forward\":" + (newDirection==1);  //format the json change request
    s += "}}";
    return s;
  }
  public String getFKeyChangeJson(String fnName, int newState) {
    String s = "{\"type\":\"throttle\",\"data\":{\"throttle\":\"" + _throttleKey + "\"";
    s += ",\""+fnName+"\":" + (newState==1);  //format the json change request
    s += "}}";
    return s;
  }

  public void setFKeyState(String fnName, boolean state) {
//    Log.d(Consts.APP_NAME,"setFKeyState("+fnName+", "+state+")");
    _fKeyState.put(fnName, state);

  }
  public boolean getFKeyState(String fnName) {
//    Log.d(Consts.APP_NAME,"getFKeyState("+fnName+")");
    return _fKeyState.get(fnName);
  }

  public void addFragment(String fragmentName) {
    if (!_fragmentNames.contains(fragmentName)) {
      _fragmentNames.add(_fragmentNames.size(), fragmentName);
    }
  }
  public void removeFragment(String fragmentName) {
    if (_fragmentNames.contains(fragmentName)) {
      _fragmentNames.remove(fragmentName);
    }
  }
}
