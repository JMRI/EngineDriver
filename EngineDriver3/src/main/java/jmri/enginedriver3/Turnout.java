package jmri.enginedriver3;

/**
 * Turnout -- instance of turnout-related values, unique by system name
 */
public class Turnout {
    private String _systemName;
    private String _userName;
    private int _state;
    private String _comment;
    private boolean _inverted;

    public Turnout(String _systemName, int _state, String _userName, boolean _inverted, String _comment) {
        this._systemName = _systemName;
        this._state = _state;
        this._userName = _userName;
        this._inverted = _inverted;
        this._comment = _comment;
    }
    //return a properly-formatted json request to change this turnout to new state
    public String getChangeStateJson(int newState) {
        String s = "{\"type\":\"turnout\",\"data\":{\"name\":\"" + _systemName +
                "\",\"state\":"+ newState + "}}";  //format the json change request
        return s;
    }

    public boolean isInverted() {
        return _inverted;
    }

    public void setInverted(boolean inverted) {
        this._inverted = inverted;
    }

    public String getComment() {
        return _comment;
    }

    public void setComment(String comment) {
        this._comment = comment;
    }

    public int getState() {
        return _state;
    }

    public void setState(int state) {
        this._state = state;
    }
//TODO: add setState(String stateDesc)
//TODO: add getStateDesc()

    public String getUserName() {
        return _userName;
    }

    public void setUserName(String userName) {
        this._userName = userName;
    }

    public String getSystemName() {
        return _systemName;
    }

    public void setSystemName(String systemName) {
        this._systemName = systemName;
    }

}
