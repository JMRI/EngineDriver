package jmri.enginedriver3;

/**
 * throttle -- a single address on a single fragment
 *   same address may be on multiple fragments, and multiple addresses on a single fragment
 *   this object corresponds to a throttle as defined by the jmri json server
 *   they will be stored in a list keyed by the unique (to jmri) throttleId
 *
 */
public class Throttle {
    private String _throttleKey;  //unique name as known to jmri
    private String _fragmentName;  //name of the fragment this item belongs to
    private String _rosterId;  //if not from a roster, set to dccAddress
    private int _dccAddress;
    private float _speed;  //jmri speed, goes from 0.0 to 1.0
    private boolean _forward; //true if current dcc direction is normal, false if opposite
    private boolean _confirmed; //true after jmri has accepted this loco
    private int _speedUnits;  //how many speed intervals between stop and full (8, 10, 14, 28, 126)

    public Throttle(String _throttleKey, String _fragmentName, String _rosterId) {
        this._throttleKey = _throttleKey;
        this._fragmentName = _fragmentName;
        this._rosterId = _rosterId;
        this._dccAddress = 0;
        this._speed = (float) -1.0;
        this._forward = true;
        this._confirmed = false;  //very important, this will be set only after the server responds with this loco
        this._speedUnits = 14;  //TODO: get this from settings?
    }

    public Throttle(String _throttleKey, String _fragmentName, String _rosterId, int _dccAddress, float _speed,
                    boolean _forward, boolean _confirmed, int _speedUnits) {
        this._throttleKey = _throttleKey;
        this._fragmentName = _fragmentName;
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

    public String getFragmentName() {
        return _fragmentName;
    }

    public void setFragmentName(String _fragmentName) {
        this._fragmentName = _fragmentName;
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

    public String getVelocityChangeJson(int newDirection, float newSpeed) {
//        {"type":"throttle","data":{"throttle":"CSX754","speed":0.25}}
        String s = "{\"type\":\"throttle\",\"data\":{\"throttle\":\"" + _throttleKey + "\"";
//        if (_forward != (newDirection==1)) {
            s += ",\"forward\":" + (newDirection==1);  //format the json change request
//        }
//        if (_speed != newSpeed) {
            s += ",\"speed\":" + newSpeed;
//        }
        s += "}}";
        return s;
    }
}
