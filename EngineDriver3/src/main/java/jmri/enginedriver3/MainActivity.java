package jmri.enginedriver3;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.support.v7.app.*;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener {

    private static MainApplication mainApp; // hold pointer to mainApp
//    private ED3PagerAdapter pagerAdapter;  //set in onCreate()

//    public ActionBar mActionBar;
    private static int fragmentsPerScreen = 1; //will be changed later

    private ViewPager viewPager = null;
    private DrawerLayout drawerLayout = null;
    private ListView drawerListView = null;
    private ActionBarDrawerToggle drawerListener = null;

//    private FragmentManager fragmentManager = null;  //set in this.onCreate()
    public ActionBar actionBar = null;
    private String[] main_menu;
    private PermaFragment permaFrag = null;  //created in activity.onCreate()
    public MainActivity_Handler mainActivityHandler = null;  //set in this.onCreate()

    //this restores the previously-saved list of dynamic fragments
    // creates the permafrag the first time, then finds it on subsequent creates
    //  also sets up the actionbar and the navigation drawer
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainApp = (MainApplication) getApplication();
        Log.d(Consts.APP_NAME,"in MainActivity.onCreate() dynaFrags=" + mainApp.getDynaFrags().size());

        //restore the list of fragments
        restoreDynaFrags();

        mainActivityHandler = new MainActivity_Handler();
        FragmentManager fragmentManager = getSupportFragmentManager();
        setContentView(R.layout.main_activity);
        viewPager = (ViewPager) findViewById(R.id.mainActivityPager);
        actionBar = getSupportActionBar();

        main_menu = getResources().getStringArray(R.array.main_menu);

        drawerListView = (ListView) findViewById(R.id.drawerList);
        drawerListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, main_menu));
        drawerListView.setOnItemClickListener(new drawer_item_clicked());

        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        drawerListener = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.close_button) {
            @Override
            public void onDrawerOpened(View drawerView) {
//                Log.d(Consts.APP_NAME, "drawer opened.");
            }
            @Override
            public void onDrawerClosed(View drawerView) {
//                Log.d(Consts.APP_NAME, "drawer closed.");
            }
        };
        drawerLayout.setDrawerListener(drawerListener);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //create (or find) the nonUI fragment to handle all threads and updates
        if (savedInstanceState == null) {
            permaFrag = new PermaFragment();
            fragmentManager.beginTransaction().add(permaFrag, "PermaFragment").commit();
//            Log.d(Consts.APP_NAME, "Created the PermaFragment");
        } else {
            permaFrag = (PermaFragment) fragmentManager.findFragmentByTag("PermaFragment");
//            Log.d(Consts.APP_NAME, "Reused existing PermaFragment");
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

        ED3PagerAdapter pagerAdapter = new ED3PagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
//                Log.d(Consts.APP_NAME,"onPageScrolled "+i+" "+v+" "+i2);
            }

            @Override
            public void onPageSelected(int i) {
                actionBar.setSelectedNavigationItem(i);
                Log.d(Consts.APP_NAME, "onPageSelected " + i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        positionToTab("Connect"); //always start up with the connection tab  TODO: do this only when disconnected
//        actionBar.getTabAt(2).select();

    }

    @Override
    protected void onPause() {
        Log.d(Consts.APP_NAME,"in MainActivity.onPause() dynaFrags=" + mainApp.getDynaFrags().size());
        saveDynaFrags();
        super.onPause();
    }

    //if the hardware menu button is pressed, open/close the drawer.  Not per android specs, but
    //  it makes sense to me, particularly for existing users
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.openDrawer(Gravity.LEFT);
            } else {
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    //save the dynaFrags list to sharedprefs as a json string
    //  note: had to convert object list to HashMap, as more complicated objects caused StackOverflowError on some devices
    private void saveDynaFrags() {
        SharedPreferences sharedPreferences = this.getSharedPreferences(Consts.APP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        Type t = new TypeToken<HashMap<Integer, HashMap<String, String>>>(){}.getType();
        //make a local hashmap copy of dynaFrag List
        HashMap<Integer, HashMap<String, String>> tdf = new HashMap<Integer, HashMap<String, String>>();
        for (int i = 0; i < mainApp.getDynaFrags().size(); i++) {
            HashMap<String, String> hm=new HashMap<String, String>();
            hm.put("name", mainApp.getDynaFrags().get(i).getName());
            hm.put("type", mainApp.getDynaFrags().get(i).getType());
            hm.put("width", Integer.toString(mainApp.getDynaFrags().get(i).getWidth()));
            hm.put("data", mainApp.getDynaFrags().get(i).getData());
            tdf.put(i, hm);
        }
        //convert the hashmap to json string
        String dynaFragsJson = gson.toJson(tdf, t);
        Log.d(Consts.APP_NAME, "saving as " + dynaFragsJson);
        //save the json string as a shared pref
        sharedPreferencesEditor.putString("dynaFragsJson", dynaFragsJson);
        sharedPreferencesEditor.commit();
    }
    private void restoreDynaFrags() {
        SharedPreferences sharedPreferences = this.getSharedPreferences(Consts.APP_NAME, Context.MODE_PRIVATE);
        String dynaFragsJson = sharedPreferences.getString("dynaFragsJson",
                "{\"0\":{\"data\":\"file:///android_asset/about_page.html\",\"type\":\"web\",\"width\":\"2\",\"name\":\"About\"},\"1\":{\"type\":\"connect\",\"width\":\"2\",\"name\":\"Connect\"},\"2\":{\"data\":\"/web/webThrottle.html\",\"type\":\"web\",\"width\":\"2\",\"name\":\"Throttle\"},\"3\":{\"type\":\"turnout\",\"width\":\"2\",\"name\":\"Turnouts\"},\"4\":{\"data\":\"/panel/\",\"type\":\"web\",\"width\":\"3\",\"name\":\"Panels\"},\"5\":{\"data\":\"/operations/trains\",\"type\":\"web\",\"width\":\"2\",\"name\":\"Trains\"}}");
        Log.d(Consts.APP_NAME, "restoring dynaFrags from " + dynaFragsJson);
        Gson gson = new Gson();
        Type t = new TypeToken<HashMap<Integer, HashMap<String, String>>>(){}.getType();
        HashMap<Integer, HashMap<String, String>> tdfhm;
        SparseArray<DynaFragEntry> tdfsa = new SparseArray<DynaFragEntry>();
        tdfhm = gson.fromJson(dynaFragsJson, t);  //restore to a simple hashmap
        for (int i = 0; i < tdfhm.size(); i++) {  //copy from the hashmap to a temp list of DynaFrags
            DynaFragEntry dfe = new DynaFragEntry(
                    tdfhm.get(i).get("name"),
                    tdfhm.get(i).get("type"),
                    Integer.valueOf(tdfhm.get(i).get("width")),
                    tdfhm.get(i).get("data"));
            tdfsa.put(i, dfe);
        }
        mainApp.setDynaFrags(tdfsa);  //replace the dynafrag list with this restored one
    }

    public class drawer_item_clicked implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//            Toast.makeText(getApplicationContext(), main_menu[i]+" ("+i+") was clicked", Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(drawerListView);
            //TODO: handle this in a less-fragile way, removing the string comparisons
            if (main_menu[i].equals("Add a Tab")) {
            } else if (main_menu[i].equals("Remove a Tab")) {
            } else if (main_menu[i].equals("Application Preferences")) {
            } else if (main_menu[i].equals("Disconnect")) {  //tell the permafrag to disconnect, if connected
                if (mainApp.getServer()!=null) {
                    mainApp.sendMsg(permaFrag.permaFragHandler, MessageType.DISCONNECT_REQUESTED);
                }
                positionToTab("Connect");
            } else if (main_menu[i].equals("Help")) {
                positionToTab("About");
            } else if (main_menu[i].equals("About")) {
                positionToTab("About");
            } else if (main_menu[i].equals("Exit")) {
                VerifyExit();
            }
        }
    }

    private void addTabs(ActionBar actionBar) {
        //build tabs for each defined fragment
        for (int i = 0; i < mainApp.getDynaFrags().size(); i++) {
            ActionBar.Tab tab = actionBar
                    .newTab()
                    .setText(mainApp.getDynaFrags().get(i).getName())
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerListener.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerListener.onConfigurationChanged(newConfig);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerListener.syncState();
    }

    @Override
    public void onBackPressed() {
        VerifyExit();
    }
    //if connected, make sure the user wants to exit
    private void VerifyExit() {
        if (mainApp.getServer()==null) {
            MainActivity.this.finish();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("Are you sure you want to disconnect and exit from EngineDriver3?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MainActivity.this.finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    class ED3PagerAdapter extends FragmentStatePagerAdapter {

        public ED3PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        //getItem() should really be called createItem()
        @Override
        public Fragment getItem(int position) {

            String t = mainApp.getDynaFrags().get(position).getType();
            String n = mainApp.getDynaFrags().get(position).getName();
            String d = mainApp.getDynaFrags().get(position).getData();
            Log.d(Consts.APP_NAME, "in MainActivity.getItem(" + position + ")" + " type " + t);

            if (t.equals(Consts.WEB)) {
                return WebViewFragment.newInstance(position, t, n, d);
            } else if (t.equals(Consts.CONSIST)) {
                return ConsistFragment.newInstance(position, t, n);
            } else if (t.equals(Consts.LIST)) {
                return DynaListFragment.newInstance(position, t, n);
            } else if (t.equals(Consts.TURNOUT)) {
                return TurnoutsFragment.newInstance(position, t, n);
            } else if (t.equals(Consts.CONNECT)) {
                return ConnectFragment.newInstance(position, t, n);
            } else {
                return DynaFragment.newInstance(position, t, n);
            }
        }

        @Override
        public int getCount() {
            return mainApp.getDynaFrags().size();
        }

        @Override
        public float getPageWidth(int position) {
            //return fraction of screen used by this fragment#, based on width
            return (float) mainApp.getDynaFrags().get(position).getWidth()/fragmentsPerScreen;
        }
    }
    //fragments send messages to here for processing and forwarding as needed.  The activity knows
    //  about all the fragments
    public class MainActivity_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.APP_NAME, "in MainActivity_Handler.handleMessage()");
            switch (msg.what) {
                case MessageType.DISCOVERED_SERVER_LIST_CHANGED:  //forward this only to the Connect fragment
                    forwardMessageToFragments(msg, Consts.CONNECT);
                    break;
                case MessageType.TURNOUT_CHANGE_REQUESTED:  //forward these only to PermaFrag for processing
                case MessageType.CONNECT_REQUESTED:
                    mainApp.sendMsg(permaFrag.permaFragHandler, msg);
                    break;
                case MessageType.CONNECTED:    //forward these to ALL active fragments
                case MessageType.DISCONNECTED:
                    forwardMessageToFragments(msg, Consts.ALL_FRAGMENTS);
                    break;
                case MessageType.TURNOUT_LIST_CHANGED:
                    forwardMessageToFragments(msg, Consts.TURNOUT);
                    break;
                case MessageType.ROUTE_LIST_CHANGED:
                    forwardMessageToFragments(msg, Consts.ROUTE);
                    break;
                case MessageType.ROSTERENTRY_LIST_CHANGED:
                    forwardMessageToFragments(msg, Consts.CONSIST);
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
                    Log.w(Consts.APP_NAME, "in MainActivity_Handler.handleMessage() not handled: " + msg.what);
            }  //end of switch msg.what
            super.handleMessage(msg);
        }

        //forward message to all active fragments that match type (or ALL_FRAGMENTS)
        private void forwardMessageToFragments(Message in_msg, String in_fragmentType) {
            for (int i = 0; i < mainApp.getDynaFrags().size(); i++) {
                Handler fh = mainApp.getDynaFrags().get(i).getHandler();
                if (fh != null &&
                        (in_fragmentType.equals(Consts.ALL_FRAGMENTS) ||
                                mainApp.getDynaFrags().get(i).getType().equals(in_fragmentType))) {
                    mainApp.sendMsg(fh, in_msg);
                }
            }
        }
    }

    //find and open the tab with the requested name
    private void positionToTab(String in_tabName) {
        for (int i = 0; i < mainApp.getDynaFrags().size(); i++) {
            if (mainApp.getDynaFrags().get(i).getName().equals(in_tabName)) {
                actionBar.getTabAt(i).select();
                return;
            }
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