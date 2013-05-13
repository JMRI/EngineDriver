package jmri.enginedriver.enginedriver3;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import android.app.Activity;
import com.actionbarsherlock.app.SherlockFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

public class TabListener<T extends SherlockFragment> implements com.actionbarsherlock.app.ActionBar.TabListener {
//    private Fragment mFragment;
//    private final Activity mActivity;
    private final String mTag;
    //private final Class<T> mClass;
    private ViewPager mPager;

    /** Constructor used each time a new tab is created.
      * @param activity  The host Activity, used to instantiate the fragment
      * @param tag  The identifier tag for the fragment
      * @param clz  The fragment's Class, used to instantiate the fragment
      */
    public TabListener(Activity activity, String tag, ViewPager pager) {
//        mActivity = activity;
        mTag = tag;
        //mClass = clz;
        mPager = pager;
    }

    /* The following are each of the ActionBar.TabListener callbacks */

    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // Check if the fragment is already initialized
        /*if (mFragment == null) {
            // If not, instantiate and add it to the activity
            mFragment = Fragment.instantiate(mActivity, mClass.getName());
            ft.add(android.R.id.content, mFragment, mTag);
        } else {
            // If it exists, simply attach it in order to show it
            ft.attach(mFragment);
        }*/
    	
    	mPager.setCurrentItem(Integer.parseInt(mTag));
    	}

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        /*if (mFragment != null) {
            // Detach the fragment, because another one is being attached
            ft.detach(mFragment);
        	}*/
    	}

    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // User selected the already selected tab. Usually do nothing.
    }

}
