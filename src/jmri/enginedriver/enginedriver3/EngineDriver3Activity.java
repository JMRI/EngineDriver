package jmri.enginedriver.enginedriver3;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import jmri.enginedriver.enginedriver3.R;
import jmri.enginedriver.enginedriver3.Consts;
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class EngineDriver3Activity extends SherlockFragmentActivity {

	private ED3FragmentPagerAdapter mAdapter;
	private ViewPager mPager;
	public ActionBar mActionBar;
	private static int fragmentsPerScreen = 1; //will be changed later

	private static EngineDriver3Application mainapp; // hold pointer to mainapp

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(Consts.DEBUG_TAG,"in EngineDriver3Activity.onCreate()");

		super.onCreate(savedInstanceState);

		mainapp=(EngineDriver3Application)getApplication();

		try {
			setContentView(R.layout.main);
			mActionBar = getSupportActionBar();
			mAdapter = new ED3FragmentPagerAdapter(getSupportFragmentManager());
			mAdapter.setActionBar(mActionBar);
			mPager = (ViewPager)findViewById(R.id.pager);
			mPager.setAdapter(mAdapter);

			mPager.setOnPageChangeListener(new OnPageChangeListener() {

				@Override
				public void onPageScrollStateChanged(int arg0) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onPageSelected(int arg0) {
					Log.d("ViewPager", "onPageSelected: " + arg0);
					mActionBar.getTabAt(arg0).select();
				}
			} );

			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

			//build tabs for each defined fragment
			for (int i = 0; i < mainapp.EDFrags.size(); i++) {
				Tab tab = mActionBar
						.newTab()
						.setText(mainapp.EDFrags.get(i).getName())
						.setTabListener(
								new TabListener<SherlockFragment>(this, i + "", mPager));
				mActionBar.addTab(tab);
			}
		} catch (Exception e) {
			Log.e("ViewPager exception:", e.toString());
		}

		//for testing, hard-code the available screen width based on orientation  TODO: replace this with calculation
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			fragmentsPerScreen = 5;
			mActionBar.setDisplayShowTitleEnabled(false);  //this interferes with the actionbar's fling
		} else {
			fragmentsPerScreen = 2;
			mActionBar.setDisplayShowTitleEnabled(true);
		}

		mActionBar.getTabAt(2).select();  //always start up with the connection tab  TODO: do this only when disconnected
	}

	public static class ED3FragmentPagerAdapter extends FragmentPagerAdapter {
		ActionBar mActionBar;

		public ED3FragmentPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return mainapp.EDFrags.size();
		}

		@Override
		public SherlockFragment getItem(int position) {

			String t = mainapp.EDFrags.get(position).getType();
			String n = mainapp.EDFrags.get(position).getName();
			String d = mainapp.EDFrags.get(position).getData();
			Log.d(Consts.DEBUG_TAG, "in EngineDriver3Activity.getItem(" + position + ")" + " type " + t);

			if (t == Consts.WEB) {
				WebFragment f = null;
				f = WebFragment.newInstance(position, t, n, d);
				return f;
			} else if (t == Consts.THROTTLE) {
				ThrottleFragment f = null;
				f = ThrottleFragment.newInstance(position, t, n);
				return f;
			} else {
				ED3Fragment f = null;
				f = ED3Fragment.newInstance(position, t, n);
				return f;
			}
		}

		public void setActionBar( ActionBar bar ) {
			mActionBar = bar;
		}

		@Override
		public float getPageWidth(final int position) {
			//return fraction of screen used by this fragment#, based on width
			return (float) mainapp.EDFrags.get(position).getWidth()/fragmentsPerScreen;
		}
	}

	

}