package jmri.enginedriver3;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DynaFragment extends Fragment {

//	@Override
//	public void onAttach(Activity activity) {
//		Log.d(Consts.DEBUG_TAG, "in DynaFragment.onAttach() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
//		super.onAttach(activity);
//	}
//	@Override
//	public void onDetach() {
//		Log.d(Consts.DEBUG_TAG, "in DynaFragment.onDetach() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
//		super.onDetach();
//	}
//	@Override
//	public void onDestroy() {
//		Log.d(Consts.DEBUG_TAG, "in DynaFragment.onDestroy() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
//		super.onDestroy();
//	}
//	@Override
//	public void onPause() {
//		Log.d(Consts.DEBUG_TAG, "in DynaFragment.onPause() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
//		super.onPause();
//	}
//	@Override
//	public void onResume() {
//		Log.d(Consts.DEBUG_TAG, "in DynaFragment.onResume() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
//		super.onResume();
//	}
//	@Override
//	public void onStart() {
//		Log.d(Consts.DEBUG_TAG, "in DynaFragment.onStart() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
//		super.onStart();
//	}
//	@Override
//	public void onStop() {
//		Log.d(Consts.DEBUG_TAG, "in DynaFragment.onStop() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
//		super.onStop();
//	}
	//	public ED3Fragment() {
//		super();
//	}
	protected static MainApplication mainApp; // hold pointer to mainApp
	private int    _fragNum;  //fragment's index (key)
	private String _fragName; //fragment's title
	private String _fragType; //fragment's type (WEB, LIST,

	protected View		view; //the view object for this fragment
	
	public String getFragName() {
		return _fragName;
	}
	public void setFragName(String fragName) {
		_fragName = fragName;
	}
	public String getFragType() {
		return _fragType;
	}
	public void setFragType(String fragType) {
		_fragType = fragType;
	}
	public int getFragNum() {
		return _fragNum;
	}
	public void setFragNum(int fragNum) {
		_fragNum = fragNum;
	}

	/**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static DynaFragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.DEBUG_TAG, "in DynaFragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		DynaFragment f = new DynaFragment();
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
		setFragNum((getArguments() != null ? getArguments().getInt("fragNum") : 1));
		setFragType((getArguments() != null ? getArguments().getString("fragType") : "error"));
		setFragName((getArguments() != null ? getArguments().getString("fragName") : "error"));

//		Log.d(Consts.DEBUG_TAG, "in DynaFragment.onCreate() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
	}


	/** inflate the proper xml layout for the fragment type
	 *    runs before activity starts		 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onCreateView() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.list_fragment;  //default to list for now
		//inflate the proper layout xml and remember it in fragment
		view = inflater.inflate(rx, container, false);  
		View tv = view.findViewById(R.id.title);  //all fragment views currently have title element
		if (tv != null) {
			((TextView)tv).setText(getFragName() + " (" + getFragNum() + ")");
		}
		return view;
	}
	/**---------------------------------------------------------------------------------------------**
	 * tells the fragment that its activity has completed its own Activity.onCreate().	 
	 * Note: calls super.  Only override if additional processing is needed                           */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
//		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onActivityCreated() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		super.onActivityCreated(savedInstanceState);
		mainApp =(MainApplication)getActivity().getApplication();  //set pointer to app
		
	}
}
