package jmri.enginedriver.enginedriver3;

import com.actionbarsherlock.app.SherlockFragment;
import jmri.enginedriver.enginedriver3.R;
import jmri.enginedriver.enginedriver3.Consts;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ED3Fragment extends SherlockFragment {

//	public ED3Fragment() {
//		super();
//	}
	protected static EngineDriver3Application mainapp; // hold pointer to mainapp
	private int 	_num;  //fragment's index (key)
	private String 	_name; //fragment's title
	private String 	_type; //fragment's type (WEB, LIST, 
	protected View		view; //the view object for this fragment
	
	public String getName() {
		return _name;
	}
	public void setName(String name) {
		_name = name;
	}
	public String getType() {
		return _type;
	}
	public void setType(String type) {
		_type = type;
	}
	public int getNum() {
		return _num;
	}
	public void setNum(int num) {
		_num = num;
	}

	/**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static ED3Fragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		ED3Fragment f = new ED3Fragment();

		// Store variables for retrieval 
		Bundle args = new Bundle();
		args.putInt("fragNum", fragNum);
		args.putString("fragType", fragType);
		args.putString("fragName", fragName);
		f.setArguments(args);

		return f;
	}

	/**---------------------------------------------------------------------------------------------**
	 * Called when new fragment is created, retrieves stuff from bundle and sets instance members	 
	 * Note: only override if extra values are needed                                               */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//fetch the stored data from bundle and copy into member variables
		setNum((getArguments() != null ? getArguments().getInt("fragNum") : 1));
		setType((getArguments() != null ? getArguments().getString("fragType") : "error"));
		setName((getArguments() != null ? getArguments().getString("fragName") : "error"));

		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onCreate() for " + getName() + " (" + getNum() + ")" + " type " + getType());
	}


	/** inflate the proper xml layout for the fragment type
	 *    runs before activity starts		 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onCreateView() for " + getName() + " (" + getNum() + ")" + " type " + getType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.list_fragment;  //default to list for now
		//inflate the proper layout xml and remember it in fragment
		view = inflater.inflate(rx, container, false);  
		View tv = view.findViewById(R.id.title);  //all fragment views currently have title element
		if (tv != null) {
			((TextView)tv).setText(getName() + " (" + getNum() + ")");
		}
		return view;
	}
	/**---------------------------------------------------------------------------------------------**
	 * tells the fragment that its activity has completed its own Activity.onCreate().	 
	 * Note: calls super.  Only override if additional processing is needed                           */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onActivityCreated() for " + getName() + " (" + getNum() + ")" + " type " + getType());
		super.onActivityCreated(savedInstanceState);
		mainapp=(EngineDriver3Application)getActivity().getApplication();  //set pointer to app
		
	}
}
