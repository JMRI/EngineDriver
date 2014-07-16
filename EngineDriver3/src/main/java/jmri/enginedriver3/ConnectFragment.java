package jmri.enginedriver3;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ConnectFragment extends ED3Fragment {
    int started = 0;

    /**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static ConnectFragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.DEBUG_TAG, "in ConnectFragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		ConnectFragment f = new ConnectFragment();

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
//		Log.d(Consts.DEBUG_TAG, "in ConnectFragment.onCreateView() for " + getName() + " (" + getNum() + ")" + " type " + getType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.connect_fragment;
		//inflate the proper layout xml and remember it in fragment
		view = inflater.inflate(rx, container, false);  
		View tv = view.findViewById(R.id.title);  //all fragment views currently have title element
		if (tv != null) {
			((TextView)tv).setText(getName() + " (" + getNum() + ")");
		}
		return view;
	}
    @Override
    public void onStart() {
        started++;
        Log.d(Consts.DEBUG_TAG, "in ConnectFragment.onStart() " + started);
        super.onStart();
    }

    @Override
    public void onDestroy() {
        Log.d(Consts.DEBUG_TAG, "in ConnectFragment.onDestroy()");
        super.onDestroy();
    }

}
