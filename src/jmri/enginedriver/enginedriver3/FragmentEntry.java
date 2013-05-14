package jmri.enginedriver.enginedriver3;

//FragmentEntry defines a single fragment to be displayed by the activity
public class FragmentEntry {
	public FragmentEntry() {
		super();
		this._name = "";
		this._type = "";
		this._width = 1;
	}
	public FragmentEntry(String _name, String _type, int _width) {
		super();
		this._name = _name;
		this._type = _type;
		this._width = _width;
	}

	protected int _width = 1;
	public int getWidth() { return _width; }
	public void setWidth(int width) { _width = width; }

	protected String _name = "";
	public String getName() { return _name; }
	public void setName(String name) { _name = name; }

	protected String _type = "";
	public String getType() { return _type; }
	public void setType(String type) { _type = type; }

}
