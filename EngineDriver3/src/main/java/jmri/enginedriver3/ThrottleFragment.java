package jmri.enginedriver3;

/* ThrottleFragment
*    A UI element which has a speed control and various buttons to allow a user to control a
*    set of DCC locos treated as a "consist".
*    Expects a layout name in fragData, and uses it to load an xml file which defines a unique
*    "flavor" of throttle.  the ids in the xml must be named correctly, but otherwise the format is
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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class ThrottleFragment extends DynaFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private MainActivity mainActivity = null;

    private static Button btnSelectLoco, btnStop, btnForward, btnReverse,
            btnSpeedDecrease, btnSpeedIncrease, btnToggleDirection;
    private static TextView tvSpeedValue, tvSpeedUnits;
    private static SeekBar sbSpeedSlider;
    private static boolean touchingSlider = false;

    //throttleKeys of locos attached to this fragment, 1 is lead.  get Throttle from list
    private SparseArray<String> throttlesAttached = new SparseArray<String>();

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

        //add click listeners for all buttons
        btnSelectLoco = (Button) fragmentView.findViewById(R.id.btnSelectLoco);
        if (btnSelectLoco!=null) btnSelectLoco.setOnClickListener(this);  //call onClick()
        btnStop = (Button) fragmentView.findViewById(R.id.btnStop);
        if (btnStop!=null) btnStop.setOnClickListener(this);  //call onClick()
        btnForward = (Button) fragmentView.findViewById(R.id.btnForward);
        if (btnForward!=null) btnForward.setOnClickListener(this);  //call onClick()
        btnReverse = (Button) fragmentView.findViewById(R.id.btnReverse);
        if (btnReverse!=null) btnReverse.setOnClickListener(this);  //call onClick()
        btnSpeedDecrease = (Button) fragmentView.findViewById(R.id.btnSpeedDecrease);
        if (btnSpeedDecrease!=null) btnSpeedDecrease.setOnClickListener(this);  //call onClick()
        btnSpeedIncrease = (Button) fragmentView.findViewById(R.id.btnSpeedIncrease);
        if (btnSpeedIncrease!=null) btnSpeedIncrease.setOnClickListener(this);  //call onClick()

        tvSpeedUnits = (TextView) fragmentView.findViewById(R.id.tvSpeedUnits);
        tvSpeedValue = (TextView) fragmentView.findViewById(R.id.tvSpeedValue);

        sbSpeedSlider = (SeekBar) fragmentView.findViewById(R.id.sbSpeedSlider);
        if (sbSpeedSlider!=null) sbSpeedSlider.setOnSeekBarChangeListener(this);

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
    public void onClick(View view) {
        if (view.getId() == R.id.btnSelectLoco) {
            Log.d(Consts.APP_NAME, "in ThrottleFragment.onClick(btnSelectLoco) ");
            showSelectLocoDialog();
        } else if (view.getId() == R.id.btnForward) {
            Throttle t = getLeadThrottle();
            mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.VELOCITY_CHANGE_REQUESTED,
                    throttlesAttached.get(0), Consts.FORWARD, t.getDisplayedSpeed());
        } else if (view.getId() == R.id.btnReverse) {
            Throttle t = getLeadThrottle();
            mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.VELOCITY_CHANGE_REQUESTED,
                    throttlesAttached.get(0), Consts.REVERSE, t.getDisplayedSpeed());
        } else if (view.getId() == R.id.btnSpeedIncrease) {
            Log.d(Consts.APP_NAME, "in ThrottleFragment.onClick(btnSpeedIncrease) ");
            Throttle t = getLeadThrottle();
            int newSpeed = t.getDisplayedSpeed() + 10;
            int dir = (t.isForward() ? 1 : 0);
            mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.VELOCITY_CHANGE_REQUESTED,
                    throttlesAttached.get(0), dir, newSpeed);
        } else if (view.getId() == R.id.btnSpeedDecrease) {
            Log.d(Consts.APP_NAME, "in ThrottleFragment.onClick(btnSpeedDecrease) ");
            Throttle t = getLeadThrottle();
            int newSpeed = t.getDisplayedSpeed() - 10;
            int dir = (t.isForward() ? 1 : 0);
            mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.VELOCITY_CHANGE_REQUESTED,
                    throttlesAttached.get(0), dir, newSpeed);
        } else {
            Log.w(Consts.APP_NAME, "unhandled button clicked in ThrottleFragment.onClick() ");
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.d(Consts.APP_NAME, "in ThrottleFragment.onProgressChanged " + fromUser + " " + progress);
        if (fromUser) {
            Throttle t = getLeadThrottle();
            int dir = (t.isForward() ? 1 : 0);
            mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.VELOCITY_CHANGE_REQUESTED,
                    throttlesAttached.get(0), dir, progress);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        Log.d(Consts.APP_NAME, "in ThrottleFragment.onStartTrackingTouch) ");
        touchingSlider = true;  //finger is on slider, send changes, but don't update from server
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
//        Log.d(Consts.APP_NAME, "in ThrottleFragment.onStopTrackingTouch ");
        touchingSlider = false;  //finger is not on slider, update from server and don't send changes
    }

    private class Fragment_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageType.CONNECTED:
                    paintThrottle();
                    break;
                case MessageType.DISCONNECTED:
                    throttlesAttached.clear();
                    paintThrottle();
                    break;
                case MessageType.ROSTERENTRY_LIST_CHANGED:
                    break;  //no action for now
                case MessageType.THROTTLE_CHANGED:
                    String throttleKey = msg.obj.toString();  //payload is throttleKey
                    throttlesAttached.put(0, throttleKey);  //attach it to this throttle
                    Log.d(Consts.APP_NAME, "in ThrottleFragment.handleMessage("+throttleKey+")");
                    paintThrottle();
                    break;  //no action for now
                default:
                    Log.w(Consts.APP_NAME, "in ThrottleFragment.handleMessage(" + msg.what + ") not handled");
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

    private void paintThrottle() {

        boolean anyAttached = (throttlesAttached.size() > 0);
        if (btnForward!=null)       btnForward.setEnabled(anyAttached);
        if (btnReverse!=null)       btnReverse.setEnabled(anyAttached);
        if (btnStop!=null)          btnStop.setEnabled(anyAttached);
        if (btnSpeedDecrease!=null) btnSpeedDecrease.setEnabled(anyAttached);
        if (btnSpeedIncrease!=null) btnSpeedIncrease.setEnabled(anyAttached);
        if (sbSpeedSlider!=null)    sbSpeedSlider.setEnabled(anyAttached);

        btnSelectLoco.setEnabled(mainApp.isConnected());

        //populate text for buttons and labels
        String locoText = (mainApp.isConnected() ? "Press to Select" : "Not Connected");
        int speedValue = 0;
        int maxValue = 100;
        boolean fwd = true;
        if (anyAttached) {
            Throttle t = getLeadThrottle();
            locoText = t.getRosterId();
            speedValue = t.getDisplayedSpeed();
            fwd = t.isForward();
            maxValue = t.getMaxSpeed();
        }
        btnSelectLoco.setText(locoText);
        if (tvSpeedValue != null)  tvSpeedValue.setText("" + speedValue);
        if (btnForward != null)    btnForward.setPressed(fwd);  //TODO: should this be 0 instead of null?
        if (btnReverse != null)    btnReverse.setPressed(!fwd);
        if (sbSpeedSlider!=null && !touchingSlider) {  //don't move the slider if a finger is on it
            sbSpeedSlider.setMax(maxValue);
            sbSpeedSlider.setProgress(speedValue);
        }

    }

    private Throttle getLeadThrottle() {
        return mainApp.getThrottle(throttlesAttached.get(0));
    }

    public void showSelectLocoDialog() {
        FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        SelectLocoDialogFragment selectLocoDialogFragment = new SelectLocoDialogFragment();
        selectLocoDialogFragment.setFragmentName(getFragName());  //TODO: more granularity?
        selectLocoDialogFragment.show(fragmentManager, "Select Loco(s)");
    }

}
