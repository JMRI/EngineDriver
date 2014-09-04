package jmri.enginedriver3;

/* ThrottleFragment
*    A UI element which has a speed control and various buttons to allow a user to control a
*    set of DCC locos treated as a "consist".
*    Expects a layout name in fragData, and uses it to load an xml file which defines a unique
*    "flavor" of throttle.  "throttle_[flavor].xml"
*    NOTE: the ids of the active elements in the xml must be named correctly, but otherwise the format is
*    free to provide various ways to control the loco(s).  Note that the code must be able to safely
*    ignore missing or hidden ids, as some flavors may not need the same buttons, etc.
*    This fragment uses Android messaging for bidirectional communication with the activity, and thence
*    with any threads which provide the network communication with the JMRI server.  Such details
*    should be hidden from this fragment as much as feasible.
* */
import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ThrottleFragment extends DynaFragment implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

  private MainActivity mainActivity = null;

  private Button btnSelectLoco, btnStop, btnForward, btnReverse,
      btnSpeedDecrease, btnSpeedIncrease, btnToggleDirection;
  private TextView tvSpeedValue, tvSpeedUnits;
  private SeekBar sbSpeedSlider;

  private boolean touchingSlider = false;

  private Consist consist = null;  //singleton to provide multi-throttle functions

  /**---------------------------------------------------------------------------------------------**
   * create a new fragment of this type, and populate basic settings in bundle
   * Note: this is a static method, called from the activity's getItem() when new ones are needed */
  static ThrottleFragment newInstance(int fragNum, String fragType, String fragName, String fragData) {
    Log.d(Consts.APP_NAME, "in ThrottleFragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
    ThrottleFragment f = new ThrottleFragment();

    // Store variables for retrieval
    Bundle args = new Bundle();
    args.putInt("fragNum", fragNum);
    args.putString("fragType", fragType);
    args.putString("fragName", fragName);
    args.putString("fragData", fragData);
    f.setArguments(args);

    return f;
  }

  /**---------------------------------------------------------------------------------------------**
   * inflate and populate the proper xml layout for this fragment type, based on data parm
   *    runs before activity starts, note does not call super		 */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    String fragData = (getArguments() != null ? getArguments().getString("fragData") : "");
    Log.d(Consts.APP_NAME, "in ThrottleFragment.onCreateView() for " + getFragName() + " ("
        + getFragNum() + ") type " + getFragType()+ ", data '" + fragData + "'");
    //choose the proper layout xml for this fragment's type, based on data parm
    String layoutName = "throttle_" + fragData;
    int rx = getResources().getIdentifier(layoutName, "layout", getActivity().getPackageName());
    //TODO: check for rx=0 and show an error of some sort?
    //inflate the proper layout xml and remember it in fragment
    fragmentView = inflater.inflate(rx, container, false);
    return fragmentView;
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(Consts.APP_NAME, "in ThrottleFragment.onStart()");

    consist = new Consist(mainApp);
    if (mainApp.isConnected()) {  //restore if connected
      consist.restoreFromPreferences(getFragName());
    }
    //add touch listeners for all buttons, to handle up and down as needed
    btnSelectLoco = (Button) fragmentView.findViewById(R.id.btnSelectLoco);
    btnSelectLoco.setOnTouchListener(this);
    float w = btnSelectLoco.getPaint().measureText("M");

    //other buttons are optional, so always check for existence
    btnStop = (Button) fragmentView.findViewById(R.id.btnStop);
    if (btnStop!=null) btnStop.setOnTouchListener(this);
    btnForward = (Button) fragmentView.findViewById(R.id.btnForward);
    if (btnForward!=null) btnForward.setOnTouchListener(this);
    btnReverse = (Button) fragmentView.findViewById(R.id.btnReverse);
    if (btnReverse!=null) btnReverse.setOnTouchListener(this);
    btnSpeedDecrease = (Button) fragmentView.findViewById(R.id.btnSpeedDecrease);
    if (btnSpeedDecrease!=null) btnSpeedDecrease.setOnTouchListener(this);
    btnSpeedIncrease = (Button) fragmentView.findViewById(R.id.btnSpeedIncrease);
    if (btnSpeedIncrease!=null) btnSpeedIncrease.setOnTouchListener(this);

    tvSpeedUnits = (TextView) fragmentView.findViewById(R.id.tvSpeedUnits);
    tvSpeedValue = (TextView) fragmentView.findViewById(R.id.tvSpeedValue);

    sbSpeedSlider = (SeekBar) fragmentView.findViewById(R.id.sbSpeedSlider);
    if (sbSpeedSlider!=null) sbSpeedSlider.setOnSeekBarChangeListener(this);

//        btnToggleDirection = (Button) fragmentView.findViewById(R.id.btnToggleDirection);  //TODO: add after creating new flavor
//        if (btnToggleDirection!=null) btnToggleDirection.setOnClickListener(this);  //call onClick()
    createFunctionButtons();
    paintThrottle();

    //only set this when fragment is ready to receive messages
    mainApp.setDynaFragHandler(getFragNum(), new Fragment_Handler());
  }

  private void createFunctionButtons() {
    Log.d(Consts.APP_NAME, "in ThrottleFragment.createFunctionButtons()");
    //insert consist's function buttons to fit in the available width, using the parent scroller for the overflow
    LinearLayout ll_function_buttons = (LinearLayout) fragmentView.findViewById(R.id.function_buttons_linear_layout);
    if (ll_function_buttons!=null) {  //don't add buttons unless the container is defined in this xml
      ll_function_buttons.removeAllViews();  //get rid of any leftover rows

      //calculate how many buttons will fit across, round, must be at least one
      int maxCols = Math.max((int) ((ll_function_buttons.getMeasuredWidth()
          / (mainApp.getEmWidth() * Consts.BUTTON_WIDTH_EMS)+0.5)), 1);

      int col = 0;  //current column
      LinearLayout row = null;
      //loop thru defined functions, creating rows and columns as needed to fit space
      for (int btnId = 1; btnId <= consist.getFKeyCount(); btnId++) {
        if (col==0) { //create new row when needed
          row = new LinearLayout(getActivity());
          LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT,    //width
              LinearLayout.LayoutParams.WRAP_CONTENT);   //height
//          rlp.weight = (float) 1.0;
          row.setLayoutParams(rlp);
          ll_function_buttons.addView(row);  //add this row to the parent
        }
        Button fnButton = new Button(getActivity());

        fnButton.setId(btnId + 0);  //btnId is 1 through n, since zero isn't an id
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            0,  //button width set to 0 because we're using the weight
            LinearLayout.LayoutParams.WRAP_CONTENT,  //height of the button
            (float) 1.0 / maxCols);  //spread the buttons to proportionally use the space available
//        blp.setMargins(0,0,0,0);  //minimize the margins
        fnButton.setLayoutParams(blp);
//        fnButton.setPadding(0,0,0,0);
        fnButton.setText(consist.getFKeyLabel(btnId - 1));
        fnButton.setLines(2);  //max of two lines of text
        fnButton.setOnTouchListener(this);
        consist.setFKeyButton(btnId - 1, fnButton);  //remember the button ref
        row.addView(fnButton);  //add this button to the row
        col++;  //bump the col, reset to zero if exceeds calced width
        if (col >= maxCols) {
          col = 0;
        } //if col>=max
      }  //for btnId
      if (col > 0 ) {  //add a dummy button to fill in the leftover gap on the last row
        Button fnButton = new Button(getActivity());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT,
            (float) (1.0 / maxCols) * (maxCols - col));  //button width * number of missing buttons
        fnButton.setLayoutParams(blp);
        fnButton.setEnabled(false);
        row.addView(fnButton);
      }
    } //ll_fn_btns!=null
  }


  @Override
  public void onStop() {
    Log.d(Consts.APP_NAME, "in ThrottleFragment.onStop()");
    //clear this to avoid late messages
    if (mainApp.getDynaFrags().get(getFragNum())!=null) mainApp.getDynaFrags().get(getFragNum()).setHandler(null);
    consist.saveToPreferences(getFragName());
    super.onStop();
  }
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
//        Log.d(Consts.APP_NAME, "in ThrottleFragment.onAttach()");
    this.mainActivity = (MainActivity) activity;  //save ref to the new activity
  }
  @Override
  public void onDetach() {
//        Log.d(Consts.APP_NAME, "in ThrottleFragment.onDetach()");
    this.mainActivity = null; //clear ref
    super.onDetach();
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//    Log.d(Consts.APP_NAME, "in ThrottleFragment.onProgressChanged " + fromUser + " " + progress);
    //if user caused this change by sliding the slider, send the change request to server
//        if (fromUser) {  //this doesn't work properly on VerticalSeekBar, so use the touching flag instead
    if (touchingSlider) {
      consist.sendSpeedChange(progress);
    }
  }
  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
//    Log.d(Consts.APP_NAME, "in ThrottleFragment.onStartTrackingTouch");
    touchingSlider = true;  //finger is on slider, send changes, but don't update from server
    consist.sendSpeedChange(seekBar.getProgress());  //without this, change waits until up
  }
  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
//    Log.d(Consts.APP_NAME, "in ThrottleFragment.onStopTrackingTouch");
    touchingSlider = false;  //finger is not on slider, update from server and don't send changes
//    consist.sendSpeedChange(seekBar.getProgress());
  }

  @Override  //for forward and back buttons, keep indication as pressed when releasing
  public boolean onTouch(View view, MotionEvent motionEvent) {
    //handle most actions on the DOWN press
    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
      if (view.getId() == R.id.btnSelectLoco) {
//      Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnSelectLoco) ");
        showSelectLocoDialog();

      } else if (view.getId() == R.id.btnForward) {
//      Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnForward) ");
        consist.sendDirectionChange(Consts.FORWARD);

      } else if (view.getId() == R.id.btnReverse) {
//      Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnReverse) ");
        consist.sendDirectionChange(Consts.REVERSE);

      } else if (view.getId() == R.id.btnSpeedIncrease) {
//      Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnSpeedIncrease) ");
        Throttle t = consist.getLeadThrottle();
        int newSpeed = t.getDisplayedSpeed() + t.getDisplayedSpeedIncrement();
        if (newSpeed>t.getMaxDisplayedSpeed()) newSpeed=t.getMaxDisplayedSpeed();
        consist.sendSpeedChange(newSpeed);

      } else if (view.getId() == R.id.btnSpeedDecrease) {
//      Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnSpeedDecrease) ");
        Throttle t = consist.getLeadThrottle();
        int newSpeed = t.getDisplayedSpeed() - t.getDisplayedSpeedIncrement();
        if (newSpeed<0) newSpeed=0;
        consist.sendSpeedChange(newSpeed);

      } else if (view.getId() == R.id.btnStop) {
//      Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnStop) ");
        consist.sendSpeedChange(0);

      } else if (consist.getFKeyButton(view.getId() - 1) != null) {  //is it in the function button list?
        String fnName = consist.getFKeyName(view.getId() - 1);
        boolean lockable = consist.getFKeyLockable(view.getId() - 1);
        if (!lockable) {  //if not lockable, send the ON state here, and OFF in up
          consist.sendFKeyChange(fnName, Consts.FN_BUTTON_ON);
        } else {  //send the opposite of current state
          Throttle t = consist.getLeadThrottle();
          boolean state = t.getFKeyState(fnName);
          consist.sendFKeyChange(fnName, (state ? Consts.FN_BUTTON_OFF : Consts.FN_BUTTON_ON));
        }
        return true;  //let the server response control the button appearance

      } else {
        Log.w(Consts.APP_NAME, "unhandled button "+view.getId()+" clicked in ThrottleFragment.onTouchDOWN");
      }
      //deal with a few UP actions
    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

      //don't allow the dir buttons to become unpressed, since the direction has already updated
      if ((view.getId()==R.id.btnForward || view.getId()==R.id.btnReverse)) {
//                Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouch ACTION_UP");
        return true;  //don't set appearance, let the server response set it

      } else if (consist.getFKeyButton(view.getId() - 1) != null) {  //is it in the function button list?
        String fnName = consist.getFKeyName(view.getId() - 1);
        boolean lockable = consist.getFKeyLockable(view.getId() - 1);
        if (!lockable) {  //if not lockable, send the OFF state here, ON was sent in down
          consist.sendFKeyChange(fnName, Consts.FN_BUTTON_OFF);
        } else {  //if lockable, don't send anything on up
        }
        return true;  //don't set appearance, let the server response set it

      } else {
        Log.d(Consts.APP_NAME, "unhandled button "+view.getId()+" clicked in ThrottleFragment.onTouchUP");
      }
    }
    return false;
  }

  private class Fragment_Handler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MessageType.CONNECTED:
//                    Log.d(Consts.APP_NAME, "in ThrottleFragment.handleMessage() CONNECTED frag="+getFragName());
          paintThrottle();
          break;
        case MessageType.DISCONNECTED:
          Log.d(Consts.APP_NAME, "in ThrottleFragment.handleMessage() DISCONNECTED frag="+getFragName());
          consist.clear();
          createFunctionButtons();
          paintThrottle();
          break;
        case MessageType.ROSTERENTRY_LIST_CHANGED:
          break;  //no action for now
        case MessageType.THROTTLE_CHANGED:
          String throttleKey = msg.obj.toString();  //payload is throttleKey
//          Log.d(Consts.APP_NAME, "in ThrottleFragment.ThrottleChanged("+throttleKey+") frag="+getFragName());
          if (!consist.hasThrottle(throttleKey)) {
            consist.addThrottle(throttleKey);  //append throttle to this consist if not already in
            if (throttleKey.equals(consist.getLeadThrottle().getThrottleKey())) {
              createFunctionButtons();  //if lead loco added, populate the buttons based on roster entry
            }
          }
          paintThrottle();
          break;  //no action for now
        default:
          Log.w(Consts.APP_NAME, "in ThrottleFragment.handleMessage(" + msg.what + ") not handled");
      }  //end of switch msg.what
      super.handleMessage(msg);
    }
  }

  //sets values and enables/disables throttle items
  private void paintThrottle() {
//        Log.d(Consts.APP_NAME, "in paintThrottle()");
    boolean anyAttached = !consist.isEmpty();
    if (btnForward!=null)       btnForward.setEnabled(anyAttached);
    if (btnReverse!=null)       btnReverse.setEnabled(anyAttached);
    if (btnStop!=null)          btnStop.setEnabled(anyAttached);
    if (btnSpeedDecrease!=null) btnSpeedDecrease.setEnabled(anyAttached);
    if (btnSpeedIncrease!=null) btnSpeedIncrease.setEnabled(anyAttached);
    if (sbSpeedSlider!=null)    sbSpeedSlider.setEnabled(anyAttached);
    if (tvSpeedUnits != null)   tvSpeedUnits.setEnabled(anyAttached);
    if (tvSpeedValue != null)   tvSpeedValue.setEnabled(anyAttached);

    btnSelectLoco.setEnabled(mainApp.isConnected());

    //populate text for buttons and labels
    int speedValue = 0;
    int maxValue = 100;
    boolean fwd = true;
    String speedUnitsText = "%";
    if (anyAttached) {
      Throttle t = consist.getLeadThrottle();
      speedValue = t.getDisplayedSpeed();
      fwd = t.isForward();
      maxValue = t.getMaxDisplayedSpeed();
      speedUnitsText = t.getSpeedUnitsText();
      //set the current pressed state of all buttons, from the lead throttle object
      for (int btnId = 1; btnId <= consist.getFKeyCount(); btnId++) {
        Button b = consist.getFKeyButton(btnId - 1);
        if (b!=null) {
          boolean state = t.getFKeyState(consist.getFKeyName(btnId - 1));
          b.setPressed(state);
        }
      }
    }
    btnSelectLoco.setText(getLocoText());
    if (tvSpeedValue != null)  tvSpeedValue.setText("" + speedValue);
    if (btnForward != null)    btnForward.setPressed(fwd);
    if (btnReverse != null)    btnReverse.setPressed(!fwd);
    if (sbSpeedSlider!=null && !touchingSlider) {  //don't auto-move the slider if a finger is on it
      sbSpeedSlider.setMax(maxValue);
      sbSpeedSlider.setProgress(speedValue);
    }
    if (tvSpeedUnits != null)  tvSpeedUnits.setText(speedUnitsText);

  }

  private String getLocoText() {
    String locoText = (mainApp.isConnected() ? "Press to Select" : "Not Connected");
    if (!consist.isEmpty()) {
      locoText = consist.toString();
    }
    return locoText;
  }

  public void showSelectLocoDialog() {
    FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
    SelectLocoDialogFragment selectLocoDialogFragment = new SelectLocoDialogFragment();
    selectLocoDialogFragment.setFragmentName(getFragName());  //TODO: more granularity?
    selectLocoDialogFragment.show(fragmentManager, "Select Loco(s)");
  }
}
