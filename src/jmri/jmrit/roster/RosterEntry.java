package jmri.jmrit.roster;

import java.net.URLEncoder;
import java.util.HashMap;

import jmri.enginedriver.threaded_application;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

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
	public int getMAXFNNUM() { return MAXFNNUM; }
	protected String[] functionLabels;
	protected String[] functionSelectedImages;
	protected String[] functionImages;
	boolean[] functionLockables;

	java.util.TreeMap<String,String> attributePairs;

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
			//decide which icon path to use, path was changed with jetty upgrade, 2.99.5
		    HashMap<String, String> metadata = threaded_application.metadata;  //reference global metadata 
			if (metadata != null && metadata.size() > 0 && metadata.get("JMRIVERCANON") != null && metadata.get("JMRIVERCANON").compareTo("2.99.4") > 0 ) {
				return rosterURL + URLEncoder.encode(_id).replace("+", "%20") + "/icon";  //roster servlet doesn't like the + replacements
			} 
			return resourcesURL + URLEncoder.encode(_iconFilePath);
		}
		return null;
		
	}
	public String getURL() { return _URL; }
	public String getDateUpdated() { return _dateUpdated; }

	public void setHostURLs(String s) {
		resourcesURL ="http://"+s+"/prefs/resources/";
		rosterURL 	 ="http://"+s+"/roster/";
	}
	
	public RosterEntry(Node n) {
		NamedNodeMap nm = n.getAttributes();
		for (int k=0; k<nm.getLength() ;k++) {
			if ("id".compareTo(nm.item(k).getNodeName())==0) {    			
				_id  = nm.item(k).getNodeValue();
				Log.d("Engine_Driver","RosterEntry - adding id "+_id);
				continue;
			}
			if ("fileName".compareTo(nm.item(k).getNodeName())==0) {
    			_fileName  = nm.item(k).getNodeValue();
    			continue;
    		}
    		if ("roadName".compareTo(nm.item(k).getNodeName())==0) {
    			_roadName  = nm.item(k).getNodeValue();
    			continue;
    		}
    		if ("roadNumber".compareTo(nm.item(k).getNodeName())==0) {
    			_roadNumber  = nm.item(k).getNodeValue();
    			continue;
    		}
    		if ("owner".compareTo(nm.item(k).getNodeName())==0) {
    			_owner  = nm.item(k).getNodeValue();
    			continue;
    		}
    		if ("mfg".compareTo(nm.item(k).getNodeName())==0) {
    			_mfg  = nm.item(k).getNodeValue();
    			continue;
    		}
    		if ("model".compareTo(nm.item(k).getNodeName())==0) {
    			_model  = nm.item(k).getNodeValue();
    			continue;
    		}
			if ("dccAddress".compareTo(nm.item(k).getNodeName())==0) {
				_dccAddress  = nm.item(k).getNodeValue();
				continue;
			}
			if (("imageFilePath".compareTo(nm.item(k).getNodeName())==0) && (nm.item(k).getNodeValue().length()>0)) {
				_imageFilePath  = nm.item(k).getNodeValue();
				continue;
			}
			if (("iconFilePath".compareTo(nm.item(k).getNodeName())==0) && (nm.item(k).getNodeValue().length()>0)) {
				_iconFilePath  = nm.item(k).getNodeValue();
				continue;
			}	
			if ("URL".compareTo(nm.item(k).getNodeName())==0) {
    			_URL  = nm.item(k).getNodeValue();
    			continue;
    		}
    		if ("maxSpeed".compareTo(nm.item(k).getNodeName())==0) {
    			_maxSpeedPCT  = Integer.parseInt(nm.item(k).getNodeValue());
    			continue;
    		}			
    		if ("comment".compareTo(nm.item(k).getNodeName())==0) {
    			_comment  = nm.item(k).getNodeValue();
    			continue;
    		}
		}
		NodeList nl = n.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);

			if ( "dateUpdated".compareTo(node.getNodeName()) == 0) {
    			_dateUpdated = node.getNodeValue();
//Log.d("RosterEntry ", "Adding date updated "+_dateUpdated+" / text content : "+node.getNodeValue());
    			continue;
    		}
/*    		if ( "locoaddress".compareTo(node.getNodeName()) == 0) {
    			_dccAddress = node.getNodeValue();
    			//TODO _isLongAddress=
    			//Log.d("RosterEntry ", "Adding 2nd dcc address "+_dccAddress+" / text content : "+node.getNodeValue());

    			continue;
    		}
*/    		
    		if ( "decoder".compareTo(node.getNodeName()) == 0) {
    			NamedNodeMap nnm = node.getAttributes();
    			for (int j = 0; j < nnm.getLength(); j++) {        			
    				if ("model".compareTo(nnm.item(j).getNodeName())==0) {
    					_decoderModel  = nnm.item(j).getNodeValue();
//    					Log.d("RosterEntry ", "adding decoder "+_decoderModel);
    					continue;
    				}
    				if ("family".compareTo(nnm.item(j).getNodeName())==0) {
    					_decoderFamily  = nnm.item(j).getNodeValue();
//    					Log.d("RosterEntry ", "adding decoder family "+_decoderFamily);
    					continue;
    				}
    				if ("comment".compareTo(nnm.item(j).getNodeName())==0) {
    					_decoderComment  = nnm.item(j).getNodeValue();
    					continue;
    				}
    			}
    			continue;
    		}
    		 
			if ("functionlabels".compareTo(node.getNodeName()) == 0) {
				loadFunctions(node);
				continue;
			}
			if ("attributepairs".compareTo(node.getNodeName()) == 0) {
				loadAttributes(node);
				continue;
			}
		}
	}
	
	// For Android 1.6 compatibility (else use e.getTextContent() )
	private String getTextContent(Node e){
		StringBuffer buffer = new StringBuffer(); 
		NodeList childList = e.getChildNodes(); 
		for (int i = 0; i < childList.getLength(); i++) { 
			Node child = childList.item(i); 
			if (child.getNodeType() != Node.TEXT_NODE) 
				continue; // skip non-text nodes 
			buffer.append(child.getNodeValue()); 
		} 
		return buffer.toString(); 
	}

	/**
	 * Loads function names from a 
	 * JDOM element.  Does not change values that are already present!
	 */
	public void loadFunctions(Node n) {
		if (n != null)  {
			// load function names
			NodeList nl = n.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node fn = nl.item(i);
				if ("functionlabel".compareTo(fn.getNodeName()) != 0)
					continue;
				String val = getTextContent(fn);
				NamedNodeMap nm = fn.getAttributes();
				int num = -1;
				boolean lockable = false;
				String imOff=null; String imOn =null;
				for (int k=0; k<nm.getLength() ;k++) {
					if ("num".compareTo(nm.item(k).getNodeName())==0) {    			
						num  = Integer.valueOf(nm.item(k).getNodeValue());
						continue;
					}
					if ("lockable".compareTo(nm.item(k).getNodeName())==0) {    			
						lockable  = ("true".compareTo(nm.item(k).getNodeValue()) == 0);
						continue;
					}
					if ("lockable".compareTo(nm.item(k).getNodeName())==0) {    			
						lockable  = ("true".compareTo(nm.item(k).getNodeValue()) == 0);
						continue;
					}
					if (("functionImage".compareTo(nm.item(k).getNodeName())==0) && (nm.item(k).getNodeValue().length()>0)) {	
						imOff = nm.item(k).getNodeValue();
						continue;
					}
					if (("functionImageSelected".compareTo(nm.item(k).getNodeName())==0) && (nm.item(k).getNodeValue().length()>0)) {
						imOn = nm.item(k).getNodeValue();
						continue;
					}
				}
				if (getFunctionLabel(num)==null) {
					setFunctionLabel(num, val);
					setFunctionLockable(num, lockable);
					if (imOff != null)
						setFunctionImage(num, imOff);
					if (imOn != null)
						setFunctionSelectedImage(num, imOn);
					Log.d("RosterEntry ","Setting function "+num+"("+val+") "+lockable+" "+imOff+"/"+imOn );
				}
			}
		}
	}

	/**
	 * Loads attribute key/value pairs from a 
	 * JDOM element.
	 */
	public void loadAttributes(Node e3) {
		/*        if (e3 != null)  {
            java.util.List<Element> l = e3.getChildren("keyvaluepair");
            for (int i = 0; i < l.size(); i++) {
                Element fn = l.get(i);
                String key = fn.getChild("key").getText();
                String value = fn.getChild("value").getText();
                this.putAttribute(key, value);
            }
        }*/
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
		if (fn <0 || fn >MAXFNNUM)
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
