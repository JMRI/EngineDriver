package jmri.enginedriver3;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ConsistFragment extends DynaFragment {
	
	/**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static ConsistFragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.APP_NAME, "in ConsistFragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		ConsistFragment f = new ConsistFragment();

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
		Log.d(Consts.APP_NAME, "in ConsistFragment.onCreateView() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.list_fragment;  //use list for now  TODO: make throttle view layout
		//inflate the proper layout xml and remember it in fragment
		fragmentView = inflater.inflate(rx, container, false);
		View tv = fragmentView.findViewById(R.id.title);  //all fragment views currently have title element
		if (tv != null) {
			((TextView)tv).setText(getFragName() + " (" + getFragNum() + ")");
		}
		return fragmentView;
	}

}
