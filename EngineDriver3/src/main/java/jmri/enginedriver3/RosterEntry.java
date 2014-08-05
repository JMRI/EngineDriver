package jmri.enginedriver3;
/*  RosterEntry - defines an instance of a roster entry, copied mostly from the JMRI class of the same name  */

import java.net.URLEncoder;

public class RosterEntry {
	// members to remember all the info
	protected String _fileName = null;

	protected String _id = "";
	protected String _roadName = "";
	protected String _roadNumber = "";
	protected String _mfg = "";
	protected String _owner = _defaultOwner;
	protected String _model = "";
	protected String _dccAddress = "";
	protected boolean _isLongAddress = false;
	protected String _comment = "";
	protected String _decoderModel = "";
	protected String _decoderFamily = "";
	protected String _decoderComment = "";
	protected String _dateUpdated = "";
	protected int _maxSpeedPCT = 100;
	protected String _URL = "";
	protected String _imageFilePath = "";
	protected String _iconFilePath = "";

	static private String _defaultOwner = "";       
	final static int MAXFNNUM = 28;

	private String resourcesURL = "";
	private String rosterURL	= "";

    public RosterEntry(String _id, String _dccAddress, boolean _isLongAddress, String _comment) {
        this._id = _id;
        this._dccAddress = _dccAddress;
        this._isLongAddress = _isLongAddress;
        this._comment = _comment;
    }

    public int getMAXFNNUM() { return MAXFNNUM; }
	protected String[] functionLabels;
    protected String[] soundLabels;
	protected String[] functionSelectedImages;
	protected String[] functionImages;
	protected boolean[] functionLockables;
    protected String _isShuntingOn="";

	private java.util.TreeMap<String,String> attributePairs;

	public static String getDefaultOwner() { return _defaultOwner; }
	public static void setDefaultOwner(String n) { _defaultOwner = n; }

	public String getId() { return _id; }
	public String getFileName() { return _fileName; }
	public String getRoadName() { return _roadName; }
	public String getRoadNumber() { return _roadNumber; }    
	public String getMfg() { return _mfg; }
	public String getModel() { return _model; }
	public String getOwner() { return _owner; }
	public String getDccAddress() { return _dccAddress; }
	public boolean isLongAddress() { return _isLongAddress; }
	public String getComment() { return _comment; }
	public String getDecoderModel() { return _decoderModel; }
	public String getDecoderFamily() { return _decoderFamily; }
	public String getDecoderComment() { return _decoderComment; }
	public String getImagePath() {
		if ((_imageFilePath != null) && (_imageFilePath.length()>0))
			return resourcesURL+URLEncoder.encode(_imageFilePath);
		return null;
	}
	
	public String getIconPath() {
		if ((_iconFilePath != null) && (_iconFilePath.length()>0) && (!_iconFilePath.equals("__noIcon.jpg"))) {			
            return rosterURL + URLEncoder.encode(_id).replace("+", "%20") + "/icon";  //roster servlet doesn't like the + replacements
		}
		return null;
		
	}
	public String getURL() { return _URL; }
	public String getDateUpdated() { return _dateUpdated; }

	public void setHostURLs(String s) {
		resourcesURL ="http://"+s+"/prefs/resources/";
		rosterURL 	 ="http://"+s+"/roster/";
	}
	
	public RosterEntry() {
	}
	
	/**
	 * Define label for a specific function
	 * @param fn function number, starting with 0
	 */
	public void setFunctionLabel(int fn, String label) {
		if (functionLabels == null) functionLabels = new String[MAXFNNUM+1]; // counts zero
		functionLabels[fn] = label;
	}

	/**
	 * If a label has been defined for a specific function,
	 * return it, otherwise return null.
	 * @param fn function number, starting with 0
	 */
	public String getFunctionLabel(int fn) {
		if (functionLabels == null) return null;
		if (fn < 0 || fn > MAXFNNUM)
			throw new IllegalArgumentException("number out of range: "+fn);
		return functionLabels[fn];
	}

	public void setFunctionImage(int fn, String s) {
		if (functionImages == null) functionImages = new String[MAXFNNUM+1]; // counts zero
		functionImages[fn] = s;
	}
	public String getFunctionImage(int fn) {
		if ((functionImages != null) && (functionImages.length>fn) && (functionImages[fn]!=null) && (functionImages[fn].length()>0))
			return resourcesURL+functionImages[fn];
		return null ; 
	}

	public void setFunctionSelectedImage(int fn, String s) {
		if (functionSelectedImages == null) functionSelectedImages = new String[MAXFNNUM+1]; // counts zero
		functionSelectedImages[fn] = s;
	}
	public String getFunctionSelectedImage(int fn) {
		if ((functionSelectedImages != null) && (functionSelectedImages.length>fn) && (functionSelectedImages[fn]!=null) && (functionSelectedImages[fn].length()>0))
			return resourcesURL+functionSelectedImages[fn];
		return null ; 
	}
	/**
	 * Define whether a specific function is lockable.
	 * @param fn function number, starting with 0
	 */
	public void setFunctionLockable(int fn, boolean lockable) {
		if (functionLockables == null) {
			functionLockables = new boolean[MAXFNNUM+1]; // counts zero
			for (int i = 0; i < functionLockables.length; i++) functionLockables[i] = true;
		}
		functionLockables[fn] = lockable;
	}


	/**
	 * Return the lockable state of a specific function. Defaults to true.
	 * @param fn function number, starting with 0
	 */
	public boolean getFunctionLockable(int fn) {
		if (functionLockables == null) return true;
		if (fn <0 || fn >MAXFNNUM)
			throw new IllegalArgumentException("number out of range: "+fn);
		return functionLockables[fn];
	}

	public void putAttribute(String key, String value) {
		if (attributePairs == null) attributePairs = new java.util.TreeMap<String,String>();
		attributePairs.put(key, value);
	}
	public String getAttribute(String key) {
		if (attributePairs == null) return null;
		return attributePairs.get(key);
	}

	public void deleteAttribute(String key) {
		if (attributePairs != null)
			attributePairs.remove(key);
	}
	public int getMaxSpeedPCT() {
		return _maxSpeedPCT;
	}
	
	//format a human-readable output of the values  TODO: figure out a way to loop thru properties instead
	public String toString() {
		String res = "";
		if (!_dccAddress.equals("")) res += "DCC Address: " + _dccAddress + "\n";
		if (!_roadName.equals("")) res += "Road Name: " + _roadName + "\n";
		if (!_roadNumber.equals("")) res += "Road Number: " + _roadNumber + "\n";
		if (!_owner.equals("")) res += "Owner: " + _owner + "\n";
		if (!_model.equals("")) res += "Model: " + _model + "\n";
		if (!_mfg.equals("")) res += "Mfg: " + _mfg + "\n";
		if (!_comment.equals("")) res += "Comment: " + _comment.replace("<?p?>", "\n") + "\n";  //clean up odd return encoding;
		if (!_decoderFamily.equals("")) res += "Decoder Family: " + _decoderFamily + "\n";
		if (!_decoderModel.equals("")) res += "Decoder Model: " + _decoderModel + "\n";
		if (!_decoderComment.equals("")) res += "Decoder Comment: " + _decoderComment.replace("<?p?>", "\n");  //clean up odd returns
		return res;
	}
}
