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
        if (btnSelectLoco!=null) btnSelectLoco.setOnTouchListener(this);
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

//        vsbSpeedSlider = (VerticalSeekBar) fragmentView.findViewById(R.id.vsbSpeedSlider);
//        if (vsbSpeedSlider!=null) vsbSpeedSlider.setOnSeekBarChangeListener(this);

//        btnToggleDirection = (Button) fragmentView.findViewById(R.id.btnToggleDirection);  //TODO: add after creating new flavor
//        if (btnToggleDirection!=null) btnToggleDirection.setOnClickListener(this);  //call onClick()

        paintThrottle();

        //only set this when fragment is ready to receive messages
        mainApp.setDynaFragHandler(getFragNum(), new Fragment_Handler());
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
//        Log.d(Consts.APP_NAME, "in ThrottleFragment.onProgressChanged " + fromUser + " " + progress);
        //if user caused this change by sliding the slider, send the change request to server
//        if (fromUser) {  //this doesn't work properly on VerticalSeekBar, so use the touching flag instead
        if (touchingSlider) {
            consist.sendSpeedChange(progress);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.d(Consts.APP_NAME, "in ThrottleFragment.onStartTrackingTouch) ");
        touchingSlider = true;  //finger is on slider, send changes, but don't update from server
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.d(Consts.APP_NAME, "in ThrottleFragment.onStopTrackingTouch ");
        touchingSlider = false;  //finger is not on slider, update from server and don't send changes
    }

    @Override  //for forward and back buttons, keep indication as pressed when releasing
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //handle most actions on the DOWN press
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (view.getId() == R.id.btnSelectLoco) {
//                Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnSelectLoco) ");
                showSelectLocoDialog();
            } else if (view.getId() == R.id.btnForward) {
//                Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnForward) ");
                consist.sendDirectionChange(Consts.FORWARD);
            } else if (view.getId() == R.id.btnReverse) {
//                Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnReverse) ");
                consist.sendDirectionChange(Consts.REVERSE);
            } else if (view.getId() == R.id.btnSpeedIncrease) {
//                Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnSpeedIncrease) ");
                Throttle t = consist.getLeadThrottle();
                int newSpeed = t.getDisplayedSpeed() + t.getDisplayedSpeedIncrement();
                if (newSpeed>t.getMaxDisplayedSpeed()) newSpeed=t.getMaxDisplayedSpeed();
                consist.sendSpeedChange(newSpeed);
            } else if (view.getId() == R.id.btnSpeedDecrease) {
//                Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnSpeedDecrease) ");
                Throttle t = consist.getLeadThrottle();
                int newSpeed = t.getDisplayedSpeed() - t.getDisplayedSpeedIncrement();
                if (newSpeed<0) newSpeed=0;
                consist.sendSpeedChange(newSpeed);
            } else if (view.getId() == R.id.btnStop) {
//                Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouchDOWN(btnStop) ");
                consist.sendSpeedChange(0);
            } else {
                Log.w(Consts.APP_NAME, "unhandled button clicked in ThrottleFragment.onTouchDOWN");
            }
        //deal with a few UP actions
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

            //don't allow the dir buttons to become unpressed, since the direction has already updated
            if ((view.getId()==R.id.btnForward || view.getId()==R.id.btnReverse)) {
//                Log.d(Consts.APP_NAME, "in ThrottleFragment.onTouch ACTION_UP");
                return true;
            } else {
//                Log.d(Consts.APP_NAME, "unhandled button clicked in ThrottleFragment.onTouchUP");
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
//                    Log.d(Consts.APP_NAME, "in ThrottleFragment.handleMessage() DISCONNECTED frag="+getFragName());
                    consist.clear();
                    paintThrottle();
                    break;
                case MessageType.ROSTERENTRY_LIST_CHANGED:
                    break;  //no action for now
                case MessageType.THROTTLE_CHANGED:
                    String throttleKey = msg.obj.toString();  //payload is throttleKey
                    if (!consist.hasThrottle(throttleKey)) {
                        consist.addThrottle(throttleKey);  //append throttle to this consist if not already in
                    }
//                    Log.d(Consts.APP_NAME, "in ThrottleFragment.ThrottleChanged("+throttleKey+") frag="+getFragName());
                    paintThrottle();
                    break;  //no action for now
                default:
                    Log.w(Consts.APP_NAME, "in ThrottleFragment.handleMessage(" + msg.what + ") not handled");
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

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
