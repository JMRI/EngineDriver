package jmri.enginedriver3;

/**
 * defines a jmri panel entry
 */
public class Panel {
    private String _type;
    private String _name;
    private String _userName;
    private String _URL;

    public Panel(String _type, String _name, String _userName, String _URL) {
        this._type = _type;
        this._name = _name;
        this._userName = _userName;
        this._URL = _URL;
    }
    public String get_URL() {return _URL;}
    public void set_URL(String _URL) {this._URL = _URL;}
    public String getUserName() {return _userName;}
    public void setUserName(String userName) {this._userName = userName;}
    public String getName() {return _name;}
    public void setName(String name) {
        this._name = name;
    }
    public String get_type() { return _type; }
    public void set_type(String _type) { this._type = _type; }

}
