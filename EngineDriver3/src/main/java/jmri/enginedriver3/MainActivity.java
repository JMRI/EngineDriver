package jmri.enginedriver3;


import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.support.v7.app.*;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener {

    private static MainApplication mainApp; // hold pointer to mainApp
    private ED3PagerAdapter pagerAdapter;  //set in onCreate()

//    public ActionBar mActionBar;
    private static int fragmentsPerScreen = 1; //will be changed later

    private ViewPager viewPager = null;

    private PermaFragment permaFrag = null;  //created in activity.onCreate()
    public MainActivity_Handler mainActivityHandler = null;  //set in this.onCreate()
    private FragmentManager fragmentManager = null;  //set in this.onCreate()
    public ActionBar actionBar = null;

    //this creates the permafrag the first time, then finds it on subsequent creates
    //  also setups the actionbar
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(Consts.DEBUG_TAG,"in MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        mainActivityHandler = new MainActivity_Handler();
        fragmentManager = getSupportFragmentManager();
        setContentView(R.layout.main_activity);
        viewPager = (ViewPager) findViewById(R.id.mainActivityPager);
        mainApp = (MainApplication) getApplication();
        actionBar = getSupportActionBar();

        //create (or find) the nonUI fragment to handle all threads and updates
        if (savedInstanceState == null) {
            permaFrag = new PermaFragment();
            fragmentManager.beginTransaction().add(permaFrag, "PermaFragment").commit();
//            Log.d(Consts.DEBUG_TAG, "Created the PermaFragment");
        } else {
            permaFrag = (PermaFragment) fragmentManager.findFragmentByTag("PermaFragment");
//            Log.d(Consts.DEBUG_TAG, "Reused existing PermaFragment");
        }

        //for testing, hard-code the available screen width based on orientation  TODO: replace this with calculation
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fragmentsPerScreen = 5;
//            actionBar.setDisplayShowTitleEnabled(false);  //this interferes with the actionbar's fling
        } else {
            fragmentsPerScreen = 2;
//            actionBar.setDisplayShowTitleEnabled(true);
        }

        addTabs(actionBar);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        pagerAdapter = new ED3PagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

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

        actionBar.getTabAt(2).select();  //always start up with the connection tab  TODO: do this only when disconnected

    }

    private void addTabs(ActionBar actionBar) {

        //build tabs for each defined fragment
        for (int i = 0; i < mainApp.dynaFrags.size(); i++) {
            ActionBar.Tab tab = actionBar
                    .newTab()
                    .setText(mainApp.dynaFrags.get(i).getName())
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

        //getItem() should really be called createItem()
        @Override
        public Fragment getItem(int position) {

            String t = mainApp.dynaFrags.get(position).getType();
            String n = mainApp.dynaFrags.get(position).getName();
            String d = mainApp.dynaFrags.get(position).getData();
            Log.d(Consts.DEBUG_TAG, "in MainActivity.getItem(" + position + ")" + " type " + t);

            if (t == Consts.WEB) {
                WebFragment f = null;
                f = WebFragment.newInstance(position, t, n, d);
                return f;
            } else if (t == Consts.THROTTLE) {
                ThrottleFragment f = null;
                f = ThrottleFragment.newInstance(position, t, n);
                return f;
            } else if (t == Consts.LIST) {
                DynaListFragment f = null;
                f = DynaListFragment.newInstance(position, t, n);
                return f;
            } else if (t == Consts.TURNOUT) {
                TurnoutsFragment f = null;
                f = TurnoutsFragment.newInstance(position, t, n);
                return f;
            } else if (t == Consts.CONNECT) {
                ConnectFragment f = null;
                f = ConnectFragment.newInstance(position, t, n);
                return f;
            } else {
                DynaFragment f = null;
                f = DynaFragment.newInstance(position, t, n);
                return f;
            }
        }

        @Override
        public int getCount() {
            return mainApp.dynaFrags.size();
        }

        @Override
        public float getPageWidth(int position) {
            //return fraction of screen used by this fragment#, based on width
            return (float) mainApp.dynaFrags.get(position).getWidth()/fragmentsPerScreen;
        }
    }
    //fragments send messages to here for processing and forwarding as needed.  The activity knows
    //  about all the fragments
    public class MainActivity_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.DEBUG_TAG, "in MainActivity_Handler.handleMessage()");
            switch (msg.what) {
                case MessageType.DISCOVERED_SERVER_LIST_CHANGED:  //forward this only to the Connect fragment
                    for (int i = 0; i < mainApp.dynaFrags.size(); i++) {
                        Handler fh = mainApp.dynaFrags.get(i).getHandler();
                        if (fh != null && mainApp.dynaFrags.get(i).getType().equals(Consts.CONNECT)) {
                            mainApp.sendMsg(fh, msg);
                        }
                    }
                    break;
                case MessageType.TURNOUT_CHANGE_REQUESTED:  //forward these only to PermaFrag for processing
                case MessageType.CONNECT_REQUESTED:
//            Log.d(Consts.DEBUG_TAG, "in MainActivity_Handler.handleMessage() CONNECT_REQUESTED");
                    mainApp.sendMsg(permaFrag.permaFragHandler, msg);
                    break;
                case MessageType.CONNECTED:    //forward these to all active fragments
                case MessageType.DISCONNECTED:
                    for (int i = 0; i < mainApp.dynaFrags.size(); i++) {
                        Handler fh = mainApp.dynaFrags.get(i).getHandler();
                        if (fh != null) {  //skip fragment if not active
                            mainApp.sendMsg(fh, msg);
                        }
                    }
                    break;
                case MessageType.TURNOUTS_LIST_CHANGED:
                    for (int i = 0; i < mainApp.dynaFrags.size(); i++) {
                        Handler fh = mainApp.dynaFrags.get(i).getHandler();
                        if (fh != null && mainApp.dynaFrags.get(i).getType().equals(Consts.TURNOUT)) {  //skip fragment if not active
                            mainApp.sendMsg(fh, msg);
                        }
                    }
                    break;
                case MessageType.MESSAGE_LONG:
                    Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;
                case MessageType.MESSAGE_SHORT:
                    Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MessageType.JMRI_TIME_CHANGED:
                case MessageType.RAILROAD_CHANGED:
                    SetSubTitle();
                    break;
                case MessageType.HEARTBEAT:
                case MessageType.POWER_STATE_CHANGED:
                    break;
                default:
                    Log.w(Consts.DEBUG_TAG, "in MainActivity_Handler.handleMessage() not handled: " + msg.what);
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

    private void SetSubTitle() {
        String s = null;  //if neither populated, set to null to "erase" the line
        if (mainApp.getRailroad()!=null || mainApp.getJmriTime()!=null) {
            s = (mainApp.getRailroad() != null ? mainApp.getRailroad()+" - " : "") +
                    (mainApp.getJmriTime() != null ? mainApp.getJmriTime() : "");
        }
        actionBar.setSubtitle(s);
    }


}