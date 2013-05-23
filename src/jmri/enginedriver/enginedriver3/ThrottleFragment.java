package jmri.enginedriver.enginedriver3;

import android.os.Bundle;
import android.util.Log;

class ThrottleFragment extends ED3Fragment {
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
}