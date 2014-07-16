package jmri.enginedriver3;


import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.support.v7.app.*;

public class ED3Activity extends ActionBarActivity implements ActionBar.TabListener {

    private static ED3Application mainapp; // hold pointer to mainapp
//    private ED3FragmentPagerAdapter mAdapter;
//    private ViewPager mPager;
//    public ActionBar mActionBar;
    private static int fragmentsPerScreen = 1; //will be changed later

    ViewPager viewPager = null;

    RetainedTaskFragment retainedTaskFrag = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager = (ViewPager) findViewById(R.id.pager);
        mainapp = (ED3Application) getApplication();
        final ActionBar actionBar = getSupportActionBar();
        //for testing, hard-code the available screen width based on orientation  TODO: replace this with calculation
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fragmentsPerScreen = 5;
//            actionBar.setDisplayShowTitleEnabled(false);  //this interferes with the actionbar's fling
        } else {
            fragmentsPerScreen = 2;
//            actionBar.setDisplayShowTitleEnabled(true);
        }

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        addTabs(actionBar);
//TODO: turn this back on        actionBar.getTabAt(2).select();  //always start up with the connection tab  TODO: do this only when disconnected

        viewPager.setAdapter(new ED3PagerAdapter(getSupportFragmentManager()));

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
//                Log.d(Consts.DEBUG_TAG,"onPageScrolled "+i+" "+v+" "+i2);
            }

            @Override
            public void onPageSelected(int i) {
                actionBar.setSelectedNavigationItem(i);
                Log.d(Consts.DEBUG_TAG, "onPageSelected " + i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
        //create (or find) a nonUI fragment to handle async stuff
        if (savedInstanceState == null) {
            retainedTaskFrag = new RetainedTaskFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(retainedTaskFrag, "RetainedTaskFragment").commit();
            Log.d(Consts.DEBUG_TAG, "Created the RetainedTaskFragment");
        } else {
            retainedTaskFrag = (RetainedTaskFragment) getSupportFragmentManager()
                    .findFragmentByTag("RetainedTaskFragment");
            Log.d(Consts.DEBUG_TAG, "Reused existing RetainedTaskFragment");
        }

    }

    private void addTabs(ActionBar actionBar) {

        //build tabs for each defined fragment
        for (int i = 0; i < mainapp.ED3Frags.size(); i++) {
            ActionBar.Tab tab = actionBar
                    .newTab()
                    .setText(mainapp.ED3Frags.get(i).getName())
                    .setTabListener(this);
            actionBar.addTab(tab);
        }

    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
    }
//}

    class ED3PagerAdapter extends FragmentStatePagerAdapter {

        public ED3PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            String t = mainapp.ED3Frags.get(position).getType();
            String n = mainapp.ED3Frags.get(position).getName();
            String d = mainapp.ED3Frags.get(position).getData();
            Log.d(Consts.DEBUG_TAG, "in EngineDriver3Activity.getItem(" + position + ")" + " type " + t);

            if (t == Consts.WEB) {
                WebFragment f = null;
                f = WebFragment.newInstance(position, t, n, d);
                return f;
            } else if (t == Consts.THROTTLE) {
                ThrottleFragment f = null;
                f = ThrottleFragment.newInstance(position, t, n);
                return f;
            } else if (t == Consts.LIST) {
                ED3ListFragment f = null;
                f = ED3ListFragment.newInstance(position, t, n);
                return f;
            } else if (t == Consts.CONNECT) {
                ConnectFragment f = null;
                f = ConnectFragment.newInstance(position, t, n);
                return f;
            } else {
                ED3Fragment f = null;
                f = ED3Fragment.newInstance(position, t, n);
                return f;
            }
        }

        @Override
        public int getCount() {
            return mainapp.ED3Frags.size();
        }

        @Override
        public float getPageWidth(int position) {
            //return fraction of screen used by this fragment#, based on width
            return (float) mainapp.ED3Frags.get(position).getWidth()/fragmentsPerScreen;
        }
    }
}