/* Copyright (C) 2017-2026 M. Steve Todd mstevetodd@gmail.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jmri.enginedriver;

import static android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
//import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.util.cvBitCalculator;

public class dcc_ex extends AppCompatActivity implements cvBitCalculator.OnConfirmListener {
    static final String activityName = "dcc_ex";

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu overflowMenu;
    private Toolbar toolbar;
    private int result = RESULT_OK;

    private String dccexCv = "";
    private String dccexCvValue = "";
    private EditText etDccexCv;
    private EditText etDccexCvValue;

    private String dccexAddress = "";
    private EditText etDccexWriteAddressValue;

    private String dccexSendCommandValue = "";
    private EditText dccexSendCommandValueEditText;

    private LinearLayout dccexDccexWriteInfoLayout;
    private TextView dccexWriteInfoLabel;
    private String dccexInfoStr = "";

    private TextView dccexHeadingLabel;

    private TextView dccexResponsesLabel;
    private TextView dccexSendsLabel;
    private String dccexResponsesStr = "";
    private String dccexSendsStr = "";

    private Spinner dccActionTypeSpinner;

    final ArrayList<String> dccexResponsesListHtml = new ArrayList<>();
    final ArrayList<String> dccexSendsListHtml = new ArrayList<>();

    private int dccCvsIndex = 0;
    String[] dccCvsEntryValuesArray;
    String[] dccCvsEntriesArray; // display version

    private int dccCmdIndex = 0;
    String[] dccexCommonCommandsEntryValuesArray;
    String[] dccexCommonCommandsEntriesArray; // display version
    int[] dccexCommonCommandsHasParametersArray; // display version

    //    private int dccexActionTypeIndex = 0;
    String[] dccexActionTypeEntryValuesArray;
    String[] dccexActionTypeEntriesArray; // display version

//    private boolean dccexHideSends = false;

    private static final int PROGRAMMING_TRACK = 0;
    private static final int PROGRAMMING_ON_MAIN = 1;
    private static final int COMMAND_LINE = 2;
    private static final int TRACK_MANAGER = 3;

    Button readAddressButton;
    Button writeAddressButton;
    Button readCvButton;
    Button writeCvButton;
    Button sendCommandButton;
    Button previousCommandButton;
    Button nextCommandButton;
    Button writeTracksButton;
    Button joinTracksButton;
    boolean hasProgTrack = false;  // used to check if the Join button should be shown
    //    Button hideSendsButton;
    Button clearCommandsButton;

    private LinearLayout dccexProgrammingCommonCvsLayout;
    private LinearLayout dccexProgrammingAddressLayout;
    private LinearLayout dccexProgrammingCvLayout;
    //    private final LinearLayout[] dexcDccexTrackLayout = {null, null, null, null, null, null, null, null};
    private LinearLayout dccexDccexTracksLayout;
    Spinner dccexCommonCvsSpinner;

    private LinearLayout dccexDccexCommandLineLayout;
    private LinearLayout dccexDccexCommonCommandsLayout;
    Spinner dccexCommonCommandsSpinner;

    private final int[] dccexTrackTypeIndex = {1, 2, 1, 1, 1, 1, 1, 1};
    private final Button[] dccexTrackPowerButton = {null, null, null, null, null, null, null, null};
    private final Spinner[] dccexTrackTypeSpinner = {null, null, null, null, null, null, null, null};
    private final EditText[] dccexTrackTypeIdEditText = {null, null, null, null, null, null, null, null};
    private final LinearLayout[] dccexTrackTypeLayout = {null, null, null, null, null, null, null, null};

    String[] dccexTrackTypeEntryValuesArray;
    String[] dccexTrackTypeEntriesArray; // display version

//    private int dccexPreviousCommandIndex = -1;
//    ArrayList<String> dccexPreviousCommandList = new ArrayList<>();

    static final int WHICH_ADDRESS = 0;
    static final int WHICH_CV = 1;
    static final int WHICH_CV_VALUE = 2;
    static final int WHICH_COMMAND = 3;

    static final int TRACK_TYPE_OFF_NONE_INDEX = 0;
    //    static final int TRACK_TYPE_DCC_MAIN_INDEX = 1;
    static final int TRACK_TYPE_DCC_PROG_INDEX = 2;
//    static final int TRACK_TYPE_DC_INDEX = 3;
//    static final int TRACK_TYPE_DCX_INDEX = 4;

    static final String[] TRACK_TYPES = {"NONE", "MAIN", "PROG", "DC", "DCX", "AUTO", "EXT", "PROG"};
    static final boolean[] TRACK_TYPES_NEED_ID = {false, false, false, true, true, false, false, false};
    static final boolean[] TRACK_TYPES_SELECTABLE = {true, true, true, true, true, true, false, false};

    String cv29SpeedSteps;
    String cv29AnalogueMode;
    String cv29Direction;
    String cv29AddressSize;
    String cv29SpeedTable;

    ImageButton dccexCvProgrammerProgTrackButton;
    ImageButton dccexCvProgrammerPomButton;
    ImageButton dccexCommandLineButton;
    ImageButton dccexTrackManagerButton;

    float vn = 4; // DCC-EC Version number

    Button openCvCalculatorDialogButton;

    //**************************************


    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class DccExMessageHandler extends Handler {

        public DccExMessageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RECEIVED_DECODER_ADDRESS:
                    String response_str = msg.obj.toString();
                    if ((!response_str.isEmpty()) && !(response_str.charAt(0) == '-')) {  //refresh address
                        dccexAddress = response_str;
                        dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSucceeded);
                    } else {
                        dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexFailed);
                    }
                    refreshDccexView();
                    break;

                case message_type.RECEIVED_CONSIST_ADDRESS:
                    String consist_response_str = msg.obj.toString();
                    if (!consist_response_str.equals("0")) {
                        dccexInfoStr = String.format(getApplicationContext().getResources().getString(R.string.dccexInCv19Consist), consist_response_str);
                    } else {
                        dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexNotInCv19Consist);
                    }
                    refreshDccexView();
                    break;

                case message_type.RECEIVED_CV:
                    String cvResponseStr = msg.obj.toString();
                    if (!cvResponseStr.isEmpty()) {
                        String[] cvArgs = cvResponseStr.split("(\\|)");
                        if ((cvArgs[0].equals(dccexCv)) && !(cvArgs[1].charAt(0) == '-')) { // response matches what we got back
                            dccexCvValue = cvArgs[1];
                            dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSucceeded);
                            checkCv29(dccexCv, dccexCvValue);
                        } else {
                            resetTextField(WHICH_CV_VALUE);
                            dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexFailed);
                        }
                        refreshDccexView();
                    }
                    break;

                case message_type.DCCEX_COMMAND_ECHO:  // informational response
                    displayCommands(msg.obj.toString(), false);
//                    refreshDccexView();
                    refreshDccexCommandsView();
                    break;

                case message_type.DCCEX_RESPONSE:  // informational response
                    displayCommands(msg.obj.toString(), true);
//                    refreshDccexView();
                    refreshDccexCommandsView();
                    break;

                case message_type.WRITE_DECODER_SUCCESS:
                    dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSucceeded);
                    refreshDccexView();
                    break;
                case message_type.WRITE_DECODER_FAIL:
                    dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexFailed);
                    refreshDccexView();
                    break;
                case message_type.RECEIVED_TRACKS:
                    refreshDccexTracksView();
                    break;

                case message_type.REOPEN_THROTTLE:
                    if (threaded_application.currentActivity == activity_id_type.DCC_EX)
                        endThisActivity();
                    break;

                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refreshDccexView();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.SHUTDOWN:
                    shutdown();
                    break;
                case message_type.DISCONNECT:
                    disconnect();
                    break;

                case message_type.RESPONSE:    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateActionViewButton(overflowMenu, overflowMenu.findItem(R.id.powerLayoutButton));
                        }
                        if ("PXX".equals(com1)) {  // individual track power response
                            refreshDccexTracksView();
                        }
                    }
                    break;

                case message_type.ESTOP_PAUSED:
                case message_type.ESTOP_RESUMED:
                    mainapp.setEmergencyStopStateActionViewButton(overflowMenu, overflowMenu.findItem(R.id.emergency_stop_button));
                    break;

                case message_type.REFRESH_OVERFLOW_MENU:
                    refreshOverflowMenu();
                    break;

                case message_type.TERMINATE_ALL_ACTIVITIES_BAR_CONNECTION:
                case message_type.LOW_MEMORY:
                    endThisActivity();
                    break;

                default:
                    break;

            }
        }
    }

    public class ReadAddressButtonListener implements View.OnClickListener {

        public void onClick(View v) {
            dccexInfoStr = "";
            resetTextField(WHICH_ADDRESS);
            mainapp.buttonVibration();
            if (vn < 5.004046) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_DECODER_ADDRESS, "*", -1);
            } else {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.READ_DCCEX_LOCO_ADDRESS, "");
                mainapp.sendMsgDelay(mainapp.comm_msg_handler, 2000, message_type.READ_DCCEX_CONSIST_ADDRESS, "");
            }
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class WriteAddressButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexInfoStr = "";
            String addrStr = etDccexWriteAddressValue.getText().toString();
            try {
                int addr = Integer.decode(addrStr);
                if ((addr > 0) && (addr <= 10239)) {
                    dccexAddress = Integer.toString(addr);
                    mainapp.buttonVibration();
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_DECODER_ADDRESS, "", addr);
                } else {
                    resetTextField(WHICH_ADDRESS);
                }
            } catch (Exception e) {
                resetTextField(WHICH_ADDRESS);
            }
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class ReadCvButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexInfoStr = "";
            resetTextField(WHICH_CV_VALUE);
            String cvStr = etDccexCv.getText().toString();
            try {
                int cv = Integer.decode(cvStr);
                if (cv > 0) {
                    dccexCv = Integer.toString(cv);
                    mainapp.buttonVibration();
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CV, "", cv);
                    refreshDccexView();
                }
            } catch (Exception e) {
                resetTextField(WHICH_CV);
            }
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class WriteCvButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexInfoStr = "";
            String cvStr = etDccexCv.getText().toString();
            String cvValueStr = etDccexCvValue.getText().toString();
            String addrStr = etDccexWriteAddressValue.getText().toString();
            if (mainapp.dccexActionTypeIndex == PROGRAMMING_TRACK) {
                try {
                    int cv = Integer.decode(cvStr);
                    int cvValue = Integer.decode(cvValueStr);
                    if (cv > 0) {
                        dccexCv = Integer.toString(cv);
                        dccexCvValue = Integer.toString(cvValue);
                        mainapp.buttonVibration();
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_CV, cvValueStr, cv);
                    } else {
                        resetTextField(WHICH_ADDRESS);
                    }
                } catch (Exception e) {
                    resetTextField(WHICH_ADDRESS);
                }
            } else {
                try {
                    Integer cv = Integer.decode(cvStr);
                    int cvValue = Integer.decode(cvValueStr);
                    int addr = Integer.decode(addrStr);
                    if ((addr > 0) && (addr <= 10239) && (cv > 0)) {
                        dccexAddress = Integer.toString(addr);
                        dccexCv = cv.toString();
                        dccexCvValue = Integer.toString(cvValue);
                        mainapp.buttonVibration();
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_POM_CV, dccexCv + " " + dccexCvValue, addr);
                    } else {
                        resetTextField(WHICH_ADDRESS);
                    }
                } catch (Exception e) {
                    resetTextField(WHICH_ADDRESS);
                }
            }
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class SendCommandButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexInfoStr = "";
            String cmdStr = dccexSendCommandValueEditText.getText().toString();
            if ((!cmdStr.isEmpty()) && (cmdStr.charAt(0) != '<')) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_SEND_COMMAND, "<" + cmdStr + ">");

                if ((cmdStr.charAt(0) == '=') && (cmdStr.length() > 1)) // we don't get a response from a tracks command, so request an update
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

                if ((mainapp.dccexPreviousCommandList.isEmpty()) || !(mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandList.size() - 1).equals(cmdStr))) {
                    mainapp.dccexPreviousCommandList.add(cmdStr);
                    if (mainapp.dccexPreviousCommandList.size() > 20) {
                        mainapp.dccexPreviousCommandList.remove(0);
                    }
                }
                mainapp.dccexPreviousCommandIndex = mainapp.dccexPreviousCommandList.size();
            }
            resetTextField(WHICH_COMMAND);
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class PreviousCommandButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexInfoStr = "";
//            String cmdStr = dccexSendCommandValueEditText.getText().toString();
            if (mainapp.dccexPreviousCommandIndex > 0) {
                dccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandIndex - 1);
                mainapp.dccexPreviousCommandIndex--;
            } else {
                dccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandList.size() - 1);
                mainapp.dccexPreviousCommandIndex = mainapp.dccexPreviousCommandList.size() - 1;
            }
            dccexSendCommandValueEditText.setText(dccexSendCommandValue);

            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class NextCommandButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexInfoStr = "";
//            String cmdStr = dccexSendCommandValueEditText.getText().toString();
            if (mainapp.dccexPreviousCommandIndex < mainapp.dccexPreviousCommandList.size() - 1) {
                dccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandIndex + 1);
                mainapp.dccexPreviousCommandIndex++;
            } else {
                dccexSendCommandValue = mainapp.dccexPreviousCommandList.get(0);
                mainapp.dccexPreviousCommandIndex = 0;
            }
            dccexSendCommandValueEditText.setText(dccexSendCommandValue);

            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class SetTrackPowerButtonListener implements View.OnClickListener {
        final int myTrack;
        final char myTrackLetter;

        public SetTrackPowerButtonListener(int track) {
            myTrack = track;
            myTrackLetter = (char) ('A' + track);
        }

        public void onClick(View v) {
            if (mainapp.dccexTrackPower[myTrack] == 0 ) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK_POWER, ""+myTrackLetter, 1);
            } else {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK_POWER, ""+myTrackLetter, 0);
            }
        }
    }

    public class WriteTracksButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            int typeIndex;
            String type;
            int id;
            char trackLetter;

            for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
                if (mainapp.dccexTrackAvailable[i]) {
                    trackLetter = (char) ('A' + i);
                    typeIndex = dccexTrackTypeSpinner[i].getSelectedItemPosition();
                    type = TRACK_TYPES[typeIndex];
                    mainapp.dccexTrackType[i] = typeIndex;

                    if (TRACK_TYPES_SELECTABLE[typeIndex]) {
                        if (!TRACK_TYPES_NEED_ID[typeIndex]) {
                            if (type.equals("AUTO")) { // needs to be set to main first if AUTO is selected
                                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " MAIN", 0);
                            }
                            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, 0);
                        } else {
                            try {
                                id = Integer.parseInt(dccexTrackTypeIdEditText[i].getText().toString());
                                mainapp.dccexTrackId[i] = Integer.toString(id);
                                if (mainapp.dccexTrackType[i] != TRACK_TYPE_OFF_NONE_INDEX) {
                                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, id);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        refreshDccexTracksView();
                    }
                }
            }
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");
//            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS_POWER, "");
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class ClearCommandsButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexResponsesListHtml.clear();
            dccexSendsListHtml.clear();
            dccexResponsesStr = "";
            dccexSendsStr = "";
            refreshDccexView();
        }
    }

    public class JoinTracksButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            if (!mainapp.dccexJoined) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_JOIN_TRACKS, "");
                activateJoinedButton(true);
            } else {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_UNJOIN_TRACKS, "");
                activateJoinedButton(false);
            }
        }
    }

    void activateJoinedButton(boolean joined) {
        mainapp.dccexJoined = joined;
        joinTracksButton.setSelected(joined);
    }

//    public class hide_sends_button_listener implements View.OnClickListener {
//        public void onClick(View v) {
//            dccexHideSends = !dccexHideSends;
//
//            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) findViewById(R.id.dexc_DccexResponsesAndSendsLayout).getLayoutParams();
//            LinearLayout.LayoutParams responsesParams = (LinearLayout.LayoutParams) DccexResponsesScrollView.getLayoutParams();
//            LinearLayout.LayoutParams sendsParams = (LinearLayout.LayoutParams) DccexSendsScrollView.getLayoutParams();
//            int h = params.height;
//            if (dccexHideSends) {
//                DccexSendsScrollView.setVisibility(View.GONE);
//                responsesParams.height = h;
//                sendsParams.height = 0;
//                DccexResponsesScrollView.setVisibility(View.GONE);
//            } else {
//                DccexSendsScrollView.setVisibility(View.VISIBLE);
//                responsesParams.height = (int) (h * 0.7);
//                sendsParams.height = (int) (h * 0.3);
//            }
//            DccexResponsesScrollView.setLayoutParams(responsesParams);
//            DccexSendsScrollView.setLayoutParams(sendsParams);
//        }
//    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void resetTextField(int which) {
        switch (which) {
            case WHICH_ADDRESS:
                dccexAddress = "";
                etDccexWriteAddressValue.setText("");
                break;
            case WHICH_CV:
                dccexCv = "";
                etDccexCv.setText("");
                break;
            case WHICH_CV_VALUE:
                dccexCvValue = "";
                etDccexCvValue.setText("");
                break;
            case WHICH_COMMAND:
                dccexSendCommandValue = "";
                dccexSendCommandValueEditText.setText("");
        }
    }

    private void readTextField(int which) {
        switch (which) {
            case WHICH_ADDRESS:
                dccexAddress = etDccexWriteAddressValue.getText().toString();
                break;
            case WHICH_CV:
                dccexCv = etDccexCv.getText().toString();
                break;
            case WHICH_CV_VALUE:
                dccexCvValue = etDccexCvValue.getText().toString();
                break;
            case WHICH_COMMAND:
                dccexSendCommandValue = dccexSendCommandValueEditText.getText().toString();
        }
    }

    private void showHideButtons() {
        switch(mainapp.dccexActionTypeIndex) {
            case PROGRAMMING_TRACK :
            case PROGRAMMING_ON_MAIN: {
                dccexProgrammingCommonCvsLayout.setVisibility(VISIBLE);
                dccexCommonCvsSpinner.setVisibility(VISIBLE);

                dccexProgrammingAddressLayout.setVisibility(VISIBLE);
                dccexProgrammingCvLayout.setVisibility(VISIBLE);

                dccexDccexTracksLayout.setVisibility(GONE);

                dccexDccexCommandLineLayout.setVisibility(GONE);
                dccexDccexCommonCommandsLayout.setVisibility(GONE);

                dccexDccexWriteInfoLayout.setVisibility(VISIBLE);

                writeAddressButton.setEnabled(!dccexAddress.isEmpty());
                readCvButton.setEnabled(!dccexCv.isEmpty());
                if (mainapp.dccexActionTypeIndex == PROGRAMMING_TRACK) {
                    writeCvButton.setEnabled(((!dccexCv.isEmpty()) && (!dccexCvValue.isEmpty())));
                } else {
                    writeCvButton.setEnabled(((!dccexCv.isEmpty()) && (!dccexCvValue.isEmpty()) && (!dccexAddress.isEmpty())));
                }
                break;
            }
            case COMMAND_LINE: {
                dccexProgrammingCommonCvsLayout.setVisibility(GONE);
                dccexCommonCvsSpinner.setVisibility(GONE);

                dccexProgrammingAddressLayout.setVisibility(GONE);
                dccexProgrammingCvLayout.setVisibility(GONE);

                dccexDccexTracksLayout.setVisibility(GONE);

                dccexDccexCommandLineLayout.setVisibility(VISIBLE);
                dccexDccexCommonCommandsLayout.setVisibility(VISIBLE);
                
                dccexDccexWriteInfoLayout.setVisibility(VISIBLE);

                sendCommandButton.setEnabled(!dccexSendCommandValue.isEmpty());
                break;
            }
            case TRACK_MANAGER:
            default: {
                dccexProgrammingCommonCvsLayout.setVisibility(GONE);
                dccexCommonCvsSpinner.setVisibility(GONE);

                dccexProgrammingAddressLayout.setVisibility(GONE);
                dccexProgrammingCvLayout.setVisibility(GONE);

                dccexDccexTracksLayout.setVisibility(VISIBLE);

                dccexDccexCommandLineLayout.setVisibility(GONE);
                dccexDccexCommonCommandsLayout.setVisibility(GONE);

                dccexDccexWriteInfoLayout.setVisibility(GONE);

                for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
                    dccexTrackTypeIdEditText[i].setVisibility(TRACK_TYPES_NEED_ID[dccexTrackTypeIndex[i]] ? VISIBLE : GONE);
                }
                joinTracksButton.setEnabled(hasProgTrack);
            }
            sendCommandButton.setEnabled((!dccexSendCommandValue.isEmpty()) && (dccexSendCommandValue.charAt(0) != '<'));
            previousCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
            nextCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
            break;
        }
    }

    public void refreshDccexView() {

        etDccexWriteAddressValue.setText(dccexAddress);
        dccexWriteInfoLabel.setText(dccexInfoStr);
        etDccexCv.setText(dccexCv);
        etDccexCvValue.setText(dccexCvValue);
//        dccexSendCommandValueEditText.setText(dccexSendCommandValue);

        if (mainapp.dccexActionTypeIndex == PROGRAMMING_TRACK) {
            readAddressButton.setVisibility(VISIBLE);
            writeAddressButton.setVisibility(VISIBLE);
            readCvButton.setVisibility(VISIBLE);
            dccexHeadingLabel.setText(R.string.dccexHeadingCvProgrammerProgTrack);
        } else {
            readAddressButton.setVisibility(GONE);
            writeAddressButton.setVisibility(GONE);
            readCvButton.setVisibility(GONE);
            if (mainapp.dccexActionTypeIndex != TRACK_MANAGER) {
                dccexHeadingLabel.setText(R.string.dccexHeadingCvProgrammerPoM);
            } else {
                dccexHeadingLabel.setText(R.string.dccexHeadingTrackManager);
            }
        }

        refreshDccexCommandsView();

        showHideButtons();

        refreshOverflowMenu();
    }

    public void refreshDccexCommandsView() {
        dccexResponsesLabel.setText(Html.fromHtml(dccexResponsesStr));
        dccexSendsLabel.setText(Html.fromHtml(dccexSendsStr));
    }

    public void refreshDccexTracksView() {
        hasProgTrack = false;
        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            dccexTrackTypeSpinner[i].setSelection(mainapp.dccexTrackType[i]);
            dccexTrackTypeIdEditText[i].setText(mainapp.dccexTrackId[i]);
            dccexTrackTypeLayout[i].setVisibility(mainapp.dccexTrackAvailable[i] ? VISIBLE : GONE);
            if (vn >= 5.002005) {
                setPowerButton(dccexTrackPowerButton[i], mainapp.dccexTrackPower[i]);
            } else {
                dccexTrackPowerButton[i].setVisibility(GONE);
            }
            dccexTrackTypeSpinner[i].setEnabled(TRACK_TYPES_SELECTABLE[mainapp.dccexTrackType[i]]);

            if (mainapp.dccexTrackType[i]==2) hasProgTrack = true;
        }
        showHideButtons();

        refreshOverflowMenu();
    } // end refreshDccexTracksView()

    void displayCommands(String msg, boolean inbound) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String currentTime = sdf.format(new Date());

        if (inbound) {
            dccexResponsesListHtml.add("<small><small>" + currentTime + " </small></small> ◄ : <b>" + Html.escapeHtml(msg) + "</b><br />");
        } else {
//            DccexSendsListHtml.add("<small><small>" + currentTime + " </small></small> ► : &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp <i>" + Html.escapeHtml(msg) + "</i><br />");
            dccexSendsListHtml.add("<small><small>" + currentTime + " </small></small> ► : <i>" + Html.escapeHtml(msg) + "</i><br />");
        }
        if (dccexResponsesListHtml.size() > 40) {
            dccexResponsesListHtml.remove(0);
        }
        if (dccexSendsListHtml.size() > 30) {
            dccexSendsListHtml.remove(0);
        }

//        dccexResponsesStr = "<p>";
//        for (int i = 0; i < dccexResponsesListHtml.size(); i++) {
//            dccexResponsesStr = dccexResponsesListHtml.get(i) + dccexResponsesStr;
//        }
//        dccexResponsesStr = dccexResponsesStr + "</p>";
        StringBuilder sb = new StringBuilder(100);
        sb.append("<p>");
        for (int i = 0; i < dccexResponsesListHtml.size(); i++) {
            sb.insert(0, dccexResponsesListHtml.get(i));
        }
        sb.append("</p>");
        dccexResponsesStr = sb.toString();

//        dccexSendsStr = "<p>";
//        for (int i=0; i < dccexSendsListHtml.size(); i++) {
//            dccexSendsStr = dccexSendsListHtml.get(i) + dccexSendsStr;
//        }
//        dccexSendsStr = dccexSendsStr + "</p>";
        sb = new StringBuilder(100);
        sb.append("<p>");
        for (int i = 0; i < dccexSendsListHtml.size(); i++) {
            sb.insert(0, dccexSendsListHtml.get(i));
        }
        sb.append("</p>");
        dccexSendsStr = sb.toString();

    }

    // ************************************************************************************************************* //

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        mainapp.applyTheme(this);

        setContentView(R.layout.dcc_ex);

        //put pointer to this activity's handler in main app's shared variable (If needed)
        mainapp.dcc_ex_msg_handler = new DccExMessageHandler(Looper.getMainLooper());

        readAddressButton = findViewById(R.id.dexc_DccexReadAddressButton);
        ReadAddressButtonListener readAddressButtonListener = new ReadAddressButtonListener();
        readAddressButton.setOnClickListener(readAddressButtonListener);

        writeAddressButton = findViewById(R.id.dexc_DccexWriteAddressButton);
        WriteAddressButtonListener writeAddressButtonListener = new WriteAddressButtonListener();
        writeAddressButton.setOnClickListener(writeAddressButtonListener);

        etDccexWriteAddressValue = findViewById(R.id.dexc_DccexWriteAddressValue);
        etDccexWriteAddressValue.setText("");
        etDccexWriteAddressValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_ADDRESS); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        readCvButton = findViewById(R.id.dexc_DccexReadCvButton);
        ReadCvButtonListener readCvButtonListener = new ReadCvButtonListener();
        readCvButton.setOnClickListener(readCvButtonListener);

        writeCvButton = findViewById(R.id.dexc_DccexWriteCvButton);
        WriteCvButtonListener writeCvButtonListener = new WriteCvButtonListener();
        writeCvButton.setOnClickListener(writeCvButtonListener);

        etDccexCv = findViewById(R.id.dexc_DccexCv);
        etDccexCv.setText("");
        etDccexCv.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CV); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexCvValue = findViewById(R.id.dexc_DccexCvValue);
        etDccexCvValue.setText("");
        etDccexCvValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CV_VALUE); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        sendCommandButton = findViewById(R.id.dccex_dccex_send_command_button);
        SendCommandButtonListener sendCommandButtonListener = new SendCommandButtonListener();
        sendCommandButton.setOnClickListener(sendCommandButtonListener);

        dccexSendCommandValueEditText = findViewById(R.id.dccex_dccex_send_command_edit_text);
        dccexSendCommandValueEditText.setInputType(TYPE_TEXT_FLAG_AUTO_CORRECT);
        dccexSendCommandValueEditText.setText("");
        dccexSendCommandValueEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_COMMAND); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        dccexDccexWriteInfoLayout = findViewById(R.id.dccex_dccex_write_info_layout);
        dccexWriteInfoLabel = findViewById(R.id.dexc_DccexWriteInfoLabel);
        dccexWriteInfoLabel.setText("");

        dccexHeadingLabel = findViewById(R.id.dccex_HeadingLabel);

        previousCommandButton = findViewById(R.id.dccex_dccex_previous_command_button);
        PreviousCommandButtonListener previousCommandButtonListener = new PreviousCommandButtonListener();
        previousCommandButton.setOnClickListener(previousCommandButtonListener);

        nextCommandButton = findViewById(R.id.dccex_dccex_next_command_button);
        NextCommandButtonListener nextCommandButtonListener = new NextCommandButtonListener();
        nextCommandButton.setOnClickListener(nextCommandButtonListener);

        dccexResponsesLabel = findViewById(R.id.dexc_DccexResponsesLabel);
        dccexResponsesLabel.setText("");
        dccexSendsLabel = findViewById(R.id.dexc_DccexSendsLabel);
        dccexSendsLabel.setText("");

        Button closeButton = findViewById(R.id.dcc_ex_button_close);
        CloseButtonListener closeButtonListener = new CloseButtonListener();
        closeButton.setOnClickListener(closeButtonListener);

        dccCvsEntryValuesArray = this.getResources().getStringArray(R.array.dccCvsEntryValues);
        dccCvsEntriesArray = this.getResources().getStringArray(R.array.dccCvsEntries); // display version

        dccCvsIndex = 0;
        dccexCommonCvsSpinner = findViewById(R.id.dexc_dcc_cv_list);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccCvsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccexCommonCvsSpinner.setAdapter(spinner_adapter);
        dccexCommonCvsSpinner.setOnItemSelectedListener(new DccExCommonCvsSpinnerListener());
        dccexCommonCvsSpinner.setSelection(dccCvsIndex);

        dccexCommonCommandsEntryValuesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntryValues);
//        final List<String> dccCommonCommandsValuesList = new ArrayList<>(Arrays.asList(dccExCommonCommandsEntryValuesArray));
        dccexCommonCommandsEntriesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntries); // display version
//        final List<String> dccCommonCommandsEntriesList = new ArrayList<>(Arrays.asList(dccExCommonCommandsEntriesArray));
        dccexCommonCommandsHasParametersArray = this.getResources().getIntArray(R.array.dccExCommonCommandsHasParameters);

        dccCmdIndex = 0;
        dccexCommonCommandsSpinner = findViewById(R.id.dccex_dccex_common_commands_list);
        spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExCommonCommandsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccexCommonCommandsSpinner.setAdapter(spinner_adapter);
        dccexCommonCommandsSpinner.setOnItemSelectedListener(new DccExCommonCommandsSpinnerListener());
        dccexCommonCommandsSpinner.setSelection(dccCmdIndex);

        vn = mainapp.getDccexVersionNumeric();

        if (vn <= 04.002007) {  // need to remove the track manager option
            dccexActionTypeEntryValuesArray = new String[3];
            dccexActionTypeEntriesArray = new String[3];
            for (int i = 0; i < 3; i++) {
                dccexActionTypeEntryValuesArray[i] = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues)[i];
                dccexActionTypeEntriesArray[i] = this.getResources().getStringArray(R.array.dccExActionTypeEntries)[i];
            }
        } else {
            dccexActionTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues);
            dccexActionTypeEntriesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntries); // display version
        }
//        final List<String> dccActionTypeValuesList = new ArrayList<>(Arrays.asList(dccexActionTypeEntryValuesArray));
//        final List<String> dccActionTypeEntriesList = new ArrayList<>(Arrays.asList(dccexActionTypeEntriesArray));

//        mainapp.dccexActionTypeIndex = PROGRAMMING_TRACK;
        dccActionTypeSpinner = findViewById(R.id.dexc_action_type_list);
//        ArrayAdapter<?> dccActionTypeSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.dccExActionTypeEntries, android.R.layout.simple_spinner_item);
        ArrayAdapter<?> dccActionTypeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dccexActionTypeEntriesArray);
        dccActionTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccActionTypeSpinner.setAdapter(dccActionTypeSpinnerAdapter);
        dccActionTypeSpinner.setOnItemSelectedListener(new DccActionTypeSpinnerListener());
        dccActionTypeSpinner.setSelection(mainapp.dccexActionTypeIndex);

        dccexCvProgrammerProgTrackButton = findViewById(R.id.dccex_cv_programmer_prog_track_button);
        DccExNavigationButtonListener dccExNavigationButtonListener = new DccExNavigationButtonListener(0);
        dccexCvProgrammerProgTrackButton.setOnClickListener(dccExNavigationButtonListener);

        dccexCvProgrammerPomButton = findViewById(R.id.dccex_cv_programmer_pom_button);
        dccExNavigationButtonListener = new DccExNavigationButtonListener(1);
        dccexCvProgrammerPomButton.setOnClickListener(dccExNavigationButtonListener);

        dccexCommandLineButton = findViewById(R.id.dccex_command_line_button);
        dccExNavigationButtonListener = new DccExNavigationButtonListener(2);
        dccexCommandLineButton.setOnClickListener(dccExNavigationButtonListener);

        dccexTrackManagerButton = findViewById(R.id.dccex_track_manager_button);
        dccExNavigationButtonListener = new DccExNavigationButtonListener(3);
        dccexTrackManagerButton.setOnClickListener(dccExNavigationButtonListener);

        ImageButton default_functions_button = findViewById(R.id.dccex_default_functions_button);
        DccexDefaultFunctionsButtonListener dccexDefaultFunctionsButtonListener = new DccexDefaultFunctionsButtonListener(this);
        default_functions_button.setOnClickListener(dccexDefaultFunctionsButtonListener);

        dccexProgrammingCommonCvsLayout = findViewById(R.id.dccex_dccex_programming_common_cvs_layout);
        dccexProgrammingAddressLayout = findViewById(R.id.dccex_dccex_programming_address_layout);
        dccexProgrammingCvLayout = findViewById(R.id.dccex_dccex_programming_cv_layout);
        dccexDccexTracksLayout = findViewById(R.id.dccex_dccex_tracks_layout);
        
        dccexDccexCommandLineLayout = findViewById(R.id.dccex_dccex_command_line_layout);
        dccexDccexCommonCommandsLayout = findViewById(R.id.dccex_dccex_common_commands_layout);

        dccexTrackTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntryValues);
        dccexTrackTypeEntriesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntries); // display version
        if (vn <= 04.002068) {  /// need to change the NONE to OFF in track manager
            dccexTrackTypeEntriesArray[0] = "OFF";
        }

        TypedArray dccex_track_type_layout_ids = getResources().obtainTypedArray(R.array.dccex_track_type_layout_ids);
        TypedArray dccex_track_power_button_ids = getResources().obtainTypedArray(R.array.dccex_track_power_button_ids);
        TypedArray dccex_track_type_ids = getResources().obtainTypedArray(R.array.dccex_track_type_ids);
        TypedArray dccex_track_id_ids = getResources().obtainTypedArray(R.array.dccex_track_id_ids);

        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            dccexTrackTypeLayout[i] = findViewById(dccex_track_type_layout_ids.getResourceId(i,0));
            dccexTrackPowerButton[i] = findViewById(dccex_track_power_button_ids.getResourceId(i,0));
            dccexTrackTypeSpinner[i] = findViewById(dccex_track_type_ids.getResourceId(i,0));
            dccexTrackTypeIdEditText[i] = findViewById(dccex_track_id_ids.getResourceId(i,0));

            ArrayAdapter<?> track_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExTrackTypeEntries, android.R.layout.simple_spinner_item);
            track_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dccexTrackTypeSpinner[i].setAdapter(track_type_spinner_adapter);
            dccexTrackTypeSpinner[i].setOnItemSelectedListener(new TrackTypeCommonCvsSpinnerListener(dccexTrackTypeSpinner[i], i));
            dccexTrackTypeSpinner[i].setSelection(dccexTrackTypeIndex[i]);

            SetTrackPowerButtonListener  buttonListener = new SetTrackPowerButtonListener(i);
            dccexTrackPowerButton[i].setOnClickListener(buttonListener);
        }

        dccex_track_type_layout_ids.recycle();
        dccex_track_power_button_ids.recycle();
        dccex_track_type_ids.recycle();
        dccex_track_id_ids.recycle();

        writeTracksButton = findViewById(R.id.dexc_DccexWriteTracksButton);
        WriteTracksButtonListener writeTracksButtonListener = new WriteTracksButtonListener();
        writeTracksButton.setOnClickListener(writeTracksButtonListener);

        joinTracksButton = findViewById(R.id.dexc_DccexJoinTracksButton);
        JoinTracksButtonListener joinTracksButtonListener = new JoinTracksButtonListener();
        joinTracksButton.setOnClickListener(joinTracksButtonListener);

//        ScrollView dccexResponsesScrollView = findViewById(R.id.dexc_DccexResponsesScrollView);
//        ScrollView dccexSendsScrollView = findViewById(R.id.dexc_DccexSendsScrollView);

        clearCommandsButton = findViewById(R.id.dcc_ex_clearCommandsButton);
        ClearCommandsButtonListener clearCommandsButtonListener = new ClearCommandsButtonListener();
        clearCommandsButton.setOnClickListener(clearCommandsButtonListener);

//            hideSendsButton = findViewById(R.id.dexc_DccexHideSendsButton);
//            hide_sends_button_listener hideSendsClickListener = new hide_sends_button_listener();
//            hideSendsButton.setOnClickListener(hideSendsClickListener);



        openCvCalculatorDialogButton = findViewById(R.id.dexc_cvBitCalculatorButton); // Get the button from your layout
        openCvCalculatorDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCvBitCalculatorDialog();
            }
        });

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.d(threaded_application.applicationName, activityName + ": handleOnBackPressed()");
                mainapp.exitDoubleBackButtonInitiated = 0;
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    threaded_application.activityInTransition(activityName);
                    setResult(result);
                    finish();
                    connection_activity.overridePendingTransition(dcc_ex.this, R.anim.fade_in, R.anim.fade_out);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        LinearLayout screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        LinearLayout statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_dcc_ex),
                    "");
        }

    } // end onCreate

    // ************************************************************************************************************* //

    @Override
    public void onPause() {
        super.onPause();
        threaded_application.activityPaused(activityName);
    }

    @Override
    public void onResume() {
        super.onResume();
        threaded_application.activityResumed(activityName);
        mainapp.removeNotification(this.getIntent());

        //noinspection AssignmentToStaticFieldFromInstanceMethod
        threaded_application.currentActivity = activity_id_type.DCC_EX;
        if (mainapp.isForcingFinish()) { //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        activateJoinedButton(mainapp.dccexJoined);

        refreshOverflowMenu();

        mainapp.dccexScreenIsOpen = true;

        refreshDccexView();
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        mainapp.hideSoftKeyboard(this.getCurrentFocus());
        mainapp.dccexScreenIsOpen = false;
        if (mainapp.dcc_ex_msg_handler != null) {
            mainapp.dcc_ex_msg_handler.removeCallbacksAndMessages(null);
            mainapp.dcc_ex_msg_handler = null;
        } else {
            Log.d(threaded_application.applicationName, activityName + ": onDestroy(): mainapp.dcc_ex_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dcc_ex_menu, menu);
        overflowMenu = menu;

        refreshOverflowMenu();

        return super.onCreateOptionsMenu(menu);
    }

    private void refreshOverflowMenu() {
        if (overflowMenu == null) return;

        mainapp.refreshCommonOverflowMenu(overflowMenu);
        adjustToolbarSize(overflowMenu);
    }

        void endThisActivity() {
        Log.d(threaded_application.applicationName, activityName + ": endThisActivity()");
        threaded_application.activityInTransition(activityName);
        mainapp.dccexScreenIsOpen = false;
        this.finish();  //end this activity
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void disconnect() {
        this.finish();
    }

    private void shutdown() {
        this.finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public class CloseButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            endThisActivity();
        }
    }

    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        boolean rslt = mainapp.implDispatchGenericMotionEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    // listener for physical keyboard events
    // used to support the gamepad only   DPAD and key events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
//        InputDevice iDev = getDevice(event.getDeviceId());
        boolean rslt = mainapp.implDispatchKeyEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.emergency_stop_button) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlightActionView(this, overflowMenu, overflowMenu.findItem(R.id.flashlight_button));
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(overflowMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public class DccExCommonCvsSpinnerListener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            dccCvsIndex = dccexCommonCvsSpinner.getSelectedItemPosition();
            if (dccCvsIndex > 0) {
                dccexCv = dccCvsEntryValuesArray[dccCvsIndex];
                resetTextField(WHICH_CV_VALUE);
                etDccexCvValue.requestFocus();
            }
            dccCvsIndex = 0;
            dccexCommonCvsSpinner.setSelection(dccCvsIndex);
            dccexInfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                mainapp.hideSoftKeyboard(view);
            }

            refreshDccexView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class DccExCommonCommandsSpinnerListener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            dccCmdIndex = dccexCommonCommandsSpinner.getSelectedItemPosition();
            if (dccCmdIndex > 0) {
                dccexSendCommandValue = dccexCommonCommandsEntryValuesArray[dccCmdIndex];
                if (dccexCommonCommandsHasParametersArray[dccCmdIndex] > 0)
                    dccexSendCommandValue = dccexSendCommandValue + " ";
                dccexSendCommandValueEditText.setText(dccexSendCommandValue);
                dccexSendCommandValueEditText.requestFocus();
                dccexSendCommandValueEditText.setSelection(dccexSendCommandValue.length());
            }
            dccCmdIndex = 0;
            dccexCommonCommandsSpinner.setSelection(dccCmdIndex);
            dccexInfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                mainapp.hideSoftKeyboard(view);
            }

            refreshDccexView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class DccActionTypeSpinnerListener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            Spinner spinner = findViewById(R.id.dexc_action_type_list);
            mainapp.dccexActionTypeIndex = spinner.getSelectedItemPosition();
            resetTextField(WHICH_CV);
            resetTextField(WHICH_CV_VALUE);
            dccexInfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                mainapp.hideSoftKeyboard(view);
            }

            dccCvsIndex = 0;
            dccexCommonCvsSpinner.setSelection(dccCvsIndex);

            setSelectedButton(position);

            refreshDccexView();
            refreshDccexTracksView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class TrackTypeCommonCvsSpinnerListener implements AdapterView.OnItemSelectedListener {
        final Spinner mySpinner;
        final int myIndex;

        TrackTypeCommonCvsSpinnerListener(Spinner spinner, int index) {
            mySpinner = spinner;
            myIndex = index;
        }

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            dccexTrackTypeIndex[myIndex] = mySpinner.getSelectedItemPosition();
            if (dccexTrackTypeIndex[myIndex] == TRACK_TYPE_DCC_PROG_INDEX) {
                for (int i=0; i < 8; i++) {
                    if ((dccexTrackTypeIndex[i] == TRACK_TYPE_DCC_PROG_INDEX) && (myIndex != i)) { // only one prog allowed
                        dccexTrackTypeSpinner[i].setSelection(TRACK_TYPE_OFF_NONE_INDEX);
                    }
                }
            }
            if (!TRACK_TYPES_SELECTABLE[dccexTrackTypeIndex[myIndex]]) {  // invalid option. reset the lot.
                refreshDccexTracksView();
            }

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                mainapp.hideSoftKeyboard(view);
            }

            refreshDccexView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    void checkCv29(String cv, String cvValueStr) {
        if (cv.equals("29")) {
            try {
                String rslt = "";
                int cvValue = Integer.parseInt(cvValueStr);
                if (mainapp.bitExtracted(cvValue, 1, 1) == 0) {
                    cv29Direction = getApplicationContext().getResources().getString(R.string.cv29DirectionForward);
                } else {
                    cv29Direction = getApplicationContext().getResources().getString(R.string.cv29DirectionReverse);
                }
                rslt = rslt + cv29Direction + "<br />";

                if (mainapp.bitExtracted(cvValue, 1, 2) == 0) {
                    cv29SpeedSteps = getApplicationContext().getResources().getString(R.string.cv29SpeedSteps14);
                } else {
                    cv29SpeedSteps = getApplicationContext().getResources().getString(R.string.cv29SpeedSteps28);
                }
                rslt = rslt + cv29SpeedSteps + "<br />";

                if (mainapp.bitExtracted(cvValue, 1, 3) == 0) {
                    cv29AnalogueMode = getApplicationContext().getResources().getString(R.string.cv29AnalogueConversionOff);
                } else {
                    cv29AnalogueMode = getApplicationContext().getResources().getString(R.string.cv29AnalogueConversionOn);
                }
                rslt = rslt + cv29AnalogueMode + "<br />";

                // bit 3 is Railcom  (4th item)

                if (mainapp.bitExtracted(cvValue, 1, 5) == 0) {
                    cv29SpeedTable = getApplicationContext().getResources().getString(R.string.cv29SpeedTableNo);
                } else {
                    cv29SpeedTable = getApplicationContext().getResources().getString(R.string.cv29SpeedTableYes);
                }
                rslt = rslt + cv29SpeedTable + "<br />";

                if (mainapp.bitExtracted(cvValue, 1, 6) == 0) {
                    cv29AddressSize = getApplicationContext().getResources().getString(R.string.cv29AddressSize2bit);
                } else {
                    cv29AddressSize = getApplicationContext().getResources().getString(R.string.cv29AddressSize4bit);
                }
                rslt = rslt + cv29AddressSize;

                dccexResponsesStr = "<p>" + rslt + "</p>" + dccexResponsesStr;

                dccexResponsesStr = "<p>"
                        + String.format(getApplicationContext().getResources().getString(R.string.cv29SpeedToggleDirection),
                        mainapp.toggleBit(cvValue, 1))
                        + "</p>" + dccexResponsesStr;

            } catch (Exception e) {
                Log.e(threaded_application.applicationName, activityName + ": checkCv29(): Error processing cv29: " + e.getMessage());
            }
        }
    }

    void setPowerButton(Button btn, int powerState) {
        TypedValue outValue = new TypedValue();
        if (powerState == 1) {
            mainapp.theme.resolveAttribute(R.attr.ed_power_green_button, outValue, true);
        } else if (powerState == 0) {
            mainapp.theme.resolveAttribute(R.attr.ed_power_red_button, outValue, true);
        } else {
            if (!mainapp.isDCCEX) {
                mainapp.theme.resolveAttribute(R.attr.ed_power_yellow_button, outValue, true);
            } else {
                mainapp.theme.resolveAttribute(R.attr.ed_power_green_red_button, outValue, true);
            }
        }
        Drawable img = AppCompatResources.getDrawable(this, outValue.resourceId);
        btn.setBackground(img);
    }

    void setSelectedButton(int index) {
        dccexCvProgrammerProgTrackButton.setSelected(index==0);
        dccexCvProgrammerPomButton.setSelected(index==1);
        dccexCommandLineButton.setSelected(index==2);
        dccexTrackManagerButton.setSelected(index==3);
    }

    public class DccExNavigationButtonListener implements View.OnClickListener {
        final int myIndex;

        DccExNavigationButtonListener(int index) {
            myIndex = index;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
            setSelectedButton(myIndex);
            dccActionTypeSpinner.setSelection(myIndex);
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class DccexDefaultFunctionsButtonListener implements View.OnClickListener {
        final Context myContext;

        DccexDefaultFunctionsButtonListener(Context context) {
            myContext = context;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
            mainapp.hideSoftKeyboard(v);
            Intent in = new Intent().setClass(myContext, function_consist_settings.class);
            startActivity(in);
        }
    }

    void adjustToolbarSize(Menu menu) {
        int newHeightAndWidth = mainapp.adjustToolbarSize(toolbar);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            View itemChooser = item.getActionView();

            if (itemChooser != null) {
                itemChooser.getLayoutParams().height = newHeightAndWidth;
                itemChooser.getLayoutParams().width = (int) ( (float) newHeightAndWidth * 1.3 );

                itemChooser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onOptionsItemSelected(item);
                    }
                });
            }
        }
    }


    private void showCvBitCalculatorDialog() {
        int initialCv = 0;
        try {
            initialCv = Integer.parseInt(etDccexCv.getText().toString());
        } catch (Exception ignored) {
        }

        int initialValue = 0;
        try {
            initialValue = Integer.parseInt(etDccexCvValue.getText().toString());
        } catch (Exception ignored) {
        }
        cvBitCalculator cvBitCalculatorDialogFragment = cvBitCalculator.newInstance(initialValue, initialCv, mainapp.getSelectedTheme(false));
        cvBitCalculatorDialogFragment.setOnConfirmListener(this); // Set the listener
        cvBitCalculatorDialogFragment.show(getSupportFragmentManager(), "cvBitCalculatorDialogFragment");
    }

    // Implementation of the OnConfirmListener interface
    @Override
    public void onConfirm(String inputText, List<Boolean> checkboxStates) {

        // Handle the data from the dialog here
        Log.d("DCC_EX_DIALOG", "Input Text: " + inputText);
        Log.d("DCC_EX_DIALOG", "Checkbox States: " + checkboxStates.toString());

        etDccexCvValue.setText(inputText);
        dccexCvValue = etDccexCvValue.getText().toString();
    }

}
