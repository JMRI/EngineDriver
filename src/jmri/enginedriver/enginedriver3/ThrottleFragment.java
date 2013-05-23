package jmri.enginedriver.enginedriver3;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

class ThrottleFragment extends ED3Fragment {
	
	/**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static ThrottleFragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.DEBUG_TAG, "in ThrottleFragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		ThrottleFragment f = new ThrottleFragment();

		// Store variables for retrieval 
		Bundle args = new Bundle();
		args.putInt("fragNum", fragNum);
		args.putString("fragType", fragType);
		args.putString("fragName", fragName);
		f.setArguments(args);

		return f;
	}

	/**---------------------------------------------------------------------------------------------**
	 * inflate and populate the proper xml layout for this fragment type
	 *    runs before activity starts, note does not call super		 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(Consts.DEBUG_TAG, "in ThrottleFragment.onCreateView() for " + getName() + " (" + getNum() + ")" + " type " + getType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.list_fragment;  //use list for now  TODO: make throttle view layout
		//inflate the proper layout xml and remember it in fragment
		view = inflater.inflate(rx, container, false);  
		View tv = view.findViewById(R.id.title);  //all fragment views currently have title element
		if (tv != null) {
			((TextView)tv).setText(getName() + " (" + getNum() + ")");
		}
		return view;
	}

}
