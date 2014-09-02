package jmri.enginedriver3;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.support.v7.app.*;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener {

  private static MainApplication mainApp; // hold pointer to mainApp
  private static int fragmentsPerScreen = 1; //will be changed later

  private ViewPager viewPager = null;
  private DrawerLayout drawerLayout = null;
  private ListView drawerListView = null;
  private ActionBarDrawerToggle drawerListener = null;
  private ED3PagerAdapter pagerAdapter = null;
  private boolean tabsChangeInProgress = false;  //used to force fragment reload after changes

  //    private FragmentManager fragmentManager = null;  //set in this.onCreate()
  public ActionBar actionBar = null;
  private String[] main_menu;
  private PermaFragment permaFrag = null;  //created in activity.onCreate()
  public MainActivity_Handler mainActivityHandler = null;  //set in this.onCreate()

  private static final int NOTIFICATION_ID = 416;  //no significance to 416, just shouldn't be 0

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

    setupDrawerLayout();

    //create (or find) the nonUI fragment to handle all threads and updates
    if (savedInstanceState == null) {
      permaFrag = new PermaFragment();
      fragmentManager.beginTransaction().add(permaFrag, "PermaFragment").commit();
//            Log.d(Consts.APP_NAME, "Created the PermaFragment");
    } else {
      permaFrag = (PermaFragment) fragmentManager.findFragmentByTag("PermaFragment");
//            Log.d(Consts.APP_NAME, "Reused existing PermaFragment");
    }

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    int x = metrics.widthPixels;
    int y = metrics.heightPixels;
    float ratio = 1;
    if (y > x) {
      ratio = ((float) y / (float) x);
    } else
      ratio = (float) x / (float) y;

    fragmentsPerScreen = 2; //default for portrait screens
    //for testing, hard-code the available screen width based on orientation
    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      fragmentsPerScreen = (int) ((fragmentsPerScreen * ratio) + 0.5);
//            actionBar.setDisplayShowTitleEnabled(false);  //this interferes with the actionbar's fling
    }
    Log.d(Consts.APP_NAME, "Screen is " + y + "x" + x + "px, ratio is " + ratio
        + ", fragmentsPerScreen=" + fragmentsPerScreen);

    addTabs(actionBar);
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    pagerAdapter = new ED3PagerAdapter(getSupportFragmentManager());
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

  }

  @Override
  protected void onDestroy() {
    Log.d(Consts.APP_NAME, "in MainActivity.onDestroy()");
    super.onDestroy();
  }

  private void setupDrawerLayout() {
    main_menu = getResources().getStringArray(R.array.main_menu);  //TODO: improve this
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
  }

  @Override
  protected void onPause() {
    Log.d(Consts.APP_NAME,"in MainActivity.onPause() dynaFrags=" + mainApp.getDynaFrags().size());
    saveDynaFrags();
//    mainApp.addNotification(this.getIntent());
    super.onPause();
  }

  @Override
  protected void onResume() {
    Log.d(Consts.APP_NAME, "in MainActivity.onResume()");
    super.onResume();
//    mainApp.removeNotification();
  }

  @Override
  protected void onStart() {
    Log.d(Consts.APP_NAME, "in MainActivity.onStart()");
    SetSubTitle();
    super.onStart();
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
    Log.d(Consts.APP_NAME, "saving dynafrags as " + dynaFragsJson);
    //save the json string as a shared pref
    sharedPreferencesEditor.putString("dynaFragsJson", dynaFragsJson);
    sharedPreferencesEditor.commit();
  }
  private void restoreDynaFrags() {
    SharedPreferences sharedPreferences = this.getSharedPreferences(Consts.APP_NAME, Context.MODE_PRIVATE);
    String dynaFragsJson = sharedPreferences.getString("dynaFragsJson",
        "{\"0\":{\"data\":\"file:///android_asset/about_page.html\",\"type\":\"web\",\"width\":\"2\",\"name\":\"About\"},\"1\":{\"type\":\"connect\",\"width\":\"2\",\"name\":\"Connect\"},\"2\":{\"data\":\"classic\",\"type\":\"throttle\",\"width\":\"2\",\"name\":\"Throttle\"},\"3\":{\"type\":\"turnout\",\"width\":\"2\",\"name\":\"Turnouts\"},\"4\":{\"data\":\"/panel/\",\"type\":\"web\",\"width\":\"3\",\"name\":\"Panels\"},\"5\":{\"data\":\"/operations/trains\",\"type\":\"web\",\"width\":\"2\",\"name\":\"Trains\"}}");
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

  private class drawer_item_clicked implements AdapterView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//            Toast.makeText(getApplicationContext(), main_menu[i]+" ("+i+") was clicked", Toast.LENGTH_SHORT).show();
      drawerLayout.closeDrawer(drawerListView);
      //TODO: handle this in a less-fragile way, removing the string comparisons
      if (main_menu[i].equals("Add a Tab")) {
        showAddTabDialog();
      } else if (main_menu[i].equals("Remove a Tab")) {
        showRemoveTabDialog();
      } else if (main_menu[i].equals("Application Preferences")) {
      } else if (main_menu[i].equals("Disconnect")) {  //tell the permafrag to disconnect, if connected
        if (mainApp.isConnected()) {
          mainApp.sendMsg(permaFrag.permaFragHandler, MessageType.DISCONNECT_REQUESTED);
        }
        positionToTab("Connect");
      } else if (main_menu[i].equals("Help")) {
        positionToTab("About");
      } else if (main_menu[i].equals("About")) {
        positionToTab("About");
      } else if (main_menu[i].equals("Exit")) {
        verifyExit();
      }
    }
  }
  private void showRemoveTabDialog() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    RemoveTabDialogFragment removeTabDialogFragment = new RemoveTabDialogFragment();
    removeTabDialogFragment.show(fragmentManager, "Remove a Tab");
  }
  private void showAddTabDialog() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    AddTabDialogFragment addTabDialogFragment = new AddTabDialogFragment();
    addTabDialogFragment.show(fragmentManager, "Add a Tab");
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
    verifyExit();
  }
  //if connected, make sure the user wants to exit
  private void verifyExit() {
    if (!mainApp.isConnected()) {
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

  public void removeTab(String in_tabToRemove) {
    Log.d(Consts.APP_NAME, "in removeTab(" + in_tabToRemove + ")");
    //make a copy with all entries except the one to be removed
    SparseArray<DynaFragEntry> tdfsa = new SparseArray<DynaFragEntry>();
    int j=0;
    int removedTab = -1;
    for (int i = 0; i < mainApp.getDynaFrags().size(); i++) {
      DynaFragEntry tdfe = mainApp.getDynaFrags().get(i);
      if (!tdfe.getName().equals(in_tabToRemove)) {
        removedTab = i;
        tdfsa.put(j, mainApp.getDynaFrags().get(i));
        j++;
      }
    }
    mainApp.setDynaFrags(tdfsa);  //replace the list with this new, shorter one

    //this is tricky, due to the pager trying to work while the change is in-progress
    viewPager.setAdapter(null);  //disconnect pager while changing
    actionBar.removeAllTabs();  //remove all the tabs
    addTabs(actionBar);         //put back the new list of tabs
    tabsChangeInProgress = true;
    pagerAdapter.notifyDataSetChanged();  //tell the adapter you changed something, and force reload
    tabsChangeInProgress = false;
    viewPager.setAdapter(pagerAdapter);  //reconnect the pager

  }

  public void addNewTab(String in_jsonTabHashMap) {
    Log.d(Consts.APP_NAME, "in addNewTab(" + in_jsonTabHashMap + ")");

    //convert the json hashmap into a real one
    Gson gson = new Gson();
    HashMap<String, String> hm = gson.fromJson(in_jsonTabHashMap, new TypeToken<HashMap<String, String>>() {}.getType());
    String tabName = hm.get("ft_name");
    DynaFragEntry tdfe = new DynaFragEntry(tabName, //make a new frag entry
        hm.get("ft_type"), Integer.parseInt(hm.get("ft_width")), hm.get("ft_data"));

    mainApp.getDynaFrags().put(mainApp.getDynaFrags().size(), tdfe); //add this entry at end of list

    //this is tricky, due to the pager trying to work while the change is in-progress
    viewPager.setAdapter(null);  //disconnect pager while changing
    actionBar.removeAllTabs();  //remove all the tabs
    addTabs(actionBar);         //put back the new list of tabs
    tabsChangeInProgress = true;
    pagerAdapter.notifyDataSetChanged();  //tell the adapter you changed something, and force reload
    tabsChangeInProgress = false;
    viewPager.setAdapter(pagerAdapter);  //reconnect the pager

    positionToTab(tabName); //position to the new tab

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
      } else if (t.equals(Consts.THROTTLE)) {
        return ThrottleFragment.newInstance(position, t, n, d);
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
    public int getItemPosition(Object object) {
      //force reload if tab positions are changing
      if (tabsChangeInProgress) {
        return PagerAdapter.POSITION_NONE;
      }
      return super.getItemPosition(object);
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
          forwardMessageToFragmentType(msg, Consts.CONNECT);
          break;
        case MessageType.SPEED_CHANGE_REQUESTED: //forward these only to PermaFrag for processing
        case MessageType.DIRECTION_CHANGE_REQUESTED:
        case MessageType.SEND_JSON_MESSAGE:
        case MessageType.TURNOUT_CHANGE_REQUESTED:
        case MessageType.CONNECT_REQUESTED:
        case MessageType.LOCO_REQUESTED:
        case MessageType.RELEASE_LOCO_REQUESTED:
          mainApp.sendMsg(permaFrag.permaFragHandler, msg);
          break;
        case MessageType.CONNECTED:    //forward these to ALL active fragments
        case MessageType.DISCONNECTED:
          forwardMessageToFragmentType(msg, Consts.ALL_FRAGMENTS);
          break;
        case MessageType.TURNOUT_LIST_CHANGED:
          forwardMessageToFragmentType(msg, Consts.TURNOUT);
          break;
        case MessageType.ROUTE_LIST_CHANGED:
          forwardMessageToFragmentType(msg, Consts.ROUTE);
          break;
        case MessageType.ROSTERENTRY_LIST_CHANGED:
          forwardMessageToFragmentType(msg, Consts.THROTTLE);
          break;
        case MessageType.THROTTLE_CHANGED:
          Throttle t = mainApp.getThrottle(msg.obj.toString());  //throttleKey is payload
//                    Log.d(Consts.APP_NAME, "in MainActivity.ThrottleChanged(" + t.getFragmentName() + ")");
          mainActivityHandler.forwardMessageToFragmentName(msg, t.getFragmentName());  //forward to fragment which owns this throttleKey
          break;
        case MessageType.MESSAGE_LONG:  //show message to user as a toast
          Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG).show();
          // vibrate the device a bit to draw attention to this message
          Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
          vibrator.vibrate(300);  //TODO: add pref to disable this?
          break;
        case MessageType.MESSAGE_SHORT:  //show message to user as a toast
          Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
          break;
        case MessageType.JMRI_TIME_CHANGED:
        case MessageType.RAILROAD_CHANGED:
          SetSubTitle();
          break;
        case MessageType.REMOVE_TAB_REQUESTED:
          removeTab(msg.obj.toString()); //this has the tab name
          break;
        case MessageType.ADD_TAB_REQUESTED:
          addNewTab(msg.obj.toString());  //this has a json string of a hashmap of fragment settings
          break;
        case MessageType.HEARTBEAT:
        case MessageType.POWER_STATE_CHANGED:
        case MessageType.PANEL_LIST_CHANGED:
          break;  //do nothing for these
        default:
          Log.w(Consts.APP_NAME, "in MainActivity_Handler.handleMessage() not handled: " + msg.what);
      }  //end of switch msg.what
      super.handleMessage(msg);
    }

    //forward message to all active fragments that match type (or ALL_FRAGMENTS)
    private void forwardMessageToFragmentType(Message in_msg, String in_fragmentType) {
      for (int i = 0; i < mainApp.getDynaFrags().size(); i++) {
        Handler fh = mainApp.getDynaFrags().get(i).getHandler();
        if (fh != null &&
            (in_fragmentType.equals(Consts.ALL_FRAGMENTS) ||
                mainApp.getDynaFrags().get(i).getType().equals(in_fragmentType))) {
//                    Log.d(Consts.APP_NAME, "in MainActivity_Handler.handleMessage("
//                            + in_msg.what + ") " + fh.toString());
          mainApp.sendMsg(fh, in_msg);
        }
      }
    }

    //forward message to active fragment that matches name
    private void forwardMessageToFragmentName(Message in_msg, String in_fragmentName) {
      for (int i = 0; i < mainApp.getDynaFrags().size(); i++) {
        Handler fh = mainApp.getDynaFrags().get(i).getHandler();
        if (fh != null &&
            mainApp.getDynaFrags().get(i).getName().equals(in_fragmentName)) {
          mainApp.sendMsg(fh, in_msg);
          return;
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