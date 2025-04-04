/* Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.LinearLayout;
//import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jmri.enginedriver.type.message_type;
import jmri.enginedriver.util.LocaleHelper;

public class dcc_ex extends AppCompatActivity {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu menu;

    private String dccexCv = "";
    private String dccexCvValue = "";
    private EditText etDccexCv;
    private EditText etDccexCvValue;

    private String dccexAddress = "";
    private EditText etDccexWriteAddressValue;

    private String dccexSendCommandValue = "";
    private EditText etDccexSendCommandValue;

    private LinearLayout dccexWriteInfoLayout;
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
//    private static final int PROGRAMMING_ON_MAIN = 1;
    private static final int TRACK_MANAGER = 2;

    Button readAddressButton;
    Button writeAddressButton;
    Button readCvButton;
    Button writeCvButton;
    Button sendCommandButton;
    Button previousCommandButton;
    Button nextCommandButton;
    Button writeTracksButton;
    Button joinTracksButton;
    //    Button hideSendsButton;
    Button clearCommandsButton;

    private LinearLayout dexcProgrammingCommonCvsLayout;
    private LinearLayout dexcProgrammingAddressLayout;
    private LinearLayout dexcProgrammingCvLayout;
//    private final LinearLayout[] dexcDccexTracklayout = {null, null, null, null, null, null, null, null};
    private LinearLayout dexcDccexTrackLinearLayout;
    Spinner dccexCommonCvsSpinner;
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

    float vn = 4; // DCC-EC Version number

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
                        dccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexSucceeded);
                    } else {
                        dccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexFailed);
                    }
                    refreshDccexView();
                    break;
                case message_type.RECEIVED_CV:
                    String cvResponseStr = msg.obj.toString();
                    if (!cvResponseStr.isEmpty()) {
                        String[] cvArgs = cvResponseStr.split("(\\|)");
                        if ((cvArgs[0].equals(dccexCv)) && !(cvArgs[1].charAt(0) == '-')) { // response matches what we got back
                            dccexCvValue = cvArgs[1];
                            dccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexSucceeded);
                            checkCv29(dccexCv, dccexCvValue);
                        } else {
                            resetTextField(WHICH_CV_VALUE);
                            dccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexFailed);
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
                    dccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexSucceeded);
                    refreshDccexView();
                    break;
                case message_type.WRITE_DECODER_FAIL:
                    dccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexFailed);
                    refreshDccexView();
                    break;
                case message_type.RECEIVED_TRACKS:
                    refreshDccexTracksView();
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refreshDccexView();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
                case message_type.RESPONSE:    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateButton(menu);
                        }
                        if ("PXX".equals(com1)) {  // individual track power response
                            refreshDccexTracksView();
                        }
                    }
                    break;
            }
        }
    }

    public class ReadAddressButtonListener implements View.OnClickListener {

        public void onClick(View v) {
            dccexInfoStr = "";
            resetTextField(WHICH_ADDRESS);
            mainapp.buttonVibration();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_DECODER_ADDRESS, "*", -1);
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
                if ((addr > 2) && (addr <= 10239)) {
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
                    if ((addr > 2) && (addr <= 10239) && (cv > 0)) {
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
            String cmdStr = etDccexSendCommandValue.getText().toString();
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
//            String cmdStr = etDccexSendCommandValue.getText().toString();
            if (mainapp.dccexPreviousCommandIndex > 0) {
                dccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandIndex - 1);
                mainapp.dccexPreviousCommandIndex--;
            } else {
                dccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandList.size() - 1);
                mainapp.dccexPreviousCommandIndex = mainapp.dccexPreviousCommandList.size() - 1;
            }
            etDccexSendCommandValue.setText(dccexSendCommandValue);

            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class NextCommandButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexInfoStr = "";
//            String cmdStr = etDccexSendCommandValue.getText().toString();
            if (mainapp.dccexPreviousCommandIndex < mainapp.dccexPreviousCommandList.size() - 1) {
                dccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandIndex + 1);
                mainapp.dccexPreviousCommandIndex++;
            } else {
                dccexSendCommandValue = mainapp.dccexPreviousCommandList.get(0);
                mainapp.dccexPreviousCommandIndex = 0;
            }
            etDccexSendCommandValue.setText(dccexSendCommandValue);

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
                etDccexSendCommandValue.setText("");
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
                dccexSendCommandValue = etDccexSendCommandValue.getText().toString();
        }
    }

    private void showHideButtons() {
        if (mainapp.dccexActionTypeIndex != TRACK_MANAGER) {
            dexcProgrammingCommonCvsLayout.setVisibility(View.VISIBLE);
            dccexCommonCvsSpinner.setVisibility(View.VISIBLE);

            dexcProgrammingAddressLayout.setVisibility(View.VISIBLE);
            dexcProgrammingCvLayout.setVisibility(View.VISIBLE);
            dexcDccexTrackLinearLayout.setVisibility(View.GONE);
            dccexWriteInfoLayout.setVisibility(View.VISIBLE);

            sendCommandButton.setEnabled(false);
            writeAddressButton.setEnabled(!dccexAddress.isEmpty());
            readCvButton.setEnabled(!dccexCv.isEmpty());
            if (mainapp.dccexActionTypeIndex == PROGRAMMING_TRACK) {
                writeCvButton.setEnabled(((!dccexCv.isEmpty()) && (!dccexCvValue.isEmpty())));
            } else {
                writeCvButton.setEnabled(((!dccexCv.isEmpty()) && (!dccexCvValue.isEmpty()) && (!dccexAddress.isEmpty())));
            }
        } else {
            dexcProgrammingCommonCvsLayout.setVisibility(View.GONE);
            dccexCommonCvsSpinner.setVisibility(View.GONE);

            dexcProgrammingAddressLayout.setVisibility(View.GONE);
            dexcProgrammingCvLayout.setVisibility(View.GONE);
            dccexWriteInfoLayout.setVisibility(View.GONE);
            dexcDccexTrackLinearLayout.setVisibility(View.VISIBLE);

            for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
                dccexTrackTypeIdEditText[i].setVisibility(TRACK_TYPES_NEED_ID[dccexTrackTypeIndex[i]] ? View.VISIBLE : View.GONE);
            }
        }
        sendCommandButton.setEnabled((!dccexSendCommandValue.isEmpty()) && (dccexSendCommandValue.charAt(0) != '<'));
        previousCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
        nextCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
    }

    public void refreshDccexView() {

        etDccexWriteAddressValue.setText(dccexAddress);
        dccexWriteInfoLabel.setText(dccexInfoStr);
        etDccexCv.setText(dccexCv);
        etDccexCvValue.setText(dccexCvValue);
//        etDccexSendCommandValue.setText(dccexSendCommandValue);

        if (mainapp.dccexActionTypeIndex == PROGRAMMING_TRACK) {
            readAddressButton.setVisibility(View.VISIBLE);
            writeAddressButton.setVisibility(View.VISIBLE);
            readCvButton.setVisibility(View.VISIBLE);
            dccexHeadingLabel.setText(R.string.DCCEXheadingCvProgrammerProgTrack);
        } else {
            readAddressButton.setVisibility(View.GONE);
            writeAddressButton.setVisibility(View.GONE);
            readCvButton.setVisibility(View.GONE);
            if (mainapp.dccexActionTypeIndex != TRACK_MANAGER) {
                dccexHeadingLabel.setText(R.string.DCCEXheadingCvProgrammerPoM);
            } else {
                dccexHeadingLabel.setText(R.string.DCCEXheadingTrackManager);
            }
        }

        refreshDccexCommandsView();

        showHideButtons();

        if (menu != null) {
            mainapp.displayEStop(menu);
        }
    }

    public void refreshDccexCommandsView() {
        dccexResponsesLabel.setText(Html.fromHtml(dccexResponsesStr));
        dccexSendsLabel.setText(Html.fromHtml(dccexSendsStr));
    }

    public void refreshDccexTracksView() {

        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            dccexTrackTypeSpinner[i].setSelection(mainapp.dccexTrackType[i]);
            dccexTrackTypeIdEditText[i].setText(mainapp.dccexTrackId[i]);
            dccexTrackTypeLayout[i].setVisibility(mainapp.dccexTrackAvailable[i] ? View.VISIBLE : View.GONE);
            if (vn >= 5.002005) {
                setPowerButton(dccexTrackPowerButton[i], mainapp.dccexTrackPower[i]);
            } else {
                dccexTrackPowerButton[i].setVisibility(View.GONE);
            }
            dccexTrackTypeSpinner[i].setEnabled(TRACK_TYPES_SELECTABLE[mainapp.dccexTrackType[i]]);
        }
        showHideButtons();

        if (menu != null) {
            mainapp.displayEStop(menu);
        }
    }

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

        sendCommandButton = findViewById(R.id.dexc_DccexSendCommandButton);
        SendCommandButtonListener sendCommandButtonListener = new SendCommandButtonListener();
        sendCommandButton.setOnClickListener(sendCommandButtonListener);

        etDccexSendCommandValue = findViewById(R.id.dexc_DccexSendCommandValue);
        etDccexSendCommandValue.setInputType(TYPE_TEXT_FLAG_AUTO_CORRECT);
        etDccexSendCommandValue.setText("");
        etDccexSendCommandValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_COMMAND); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        dccexWriteInfoLayout = findViewById(R.id.dexc_DccexWriteInfoLayout);
        dccexWriteInfoLabel = findViewById(R.id.dexc_DccexWriteInfoLabel);
        dccexWriteInfoLabel.setText("");

        dccexHeadingLabel = findViewById(R.id.dccex_HeadingLabel);

        previousCommandButton = findViewById(R.id.dexc_DccexPreviousCommandButton);
        PreviousCommandButtonListener previousCommandButtonListener = new PreviousCommandButtonListener();
        previousCommandButton.setOnClickListener(previousCommandButtonListener);

        nextCommandButton = findViewById(R.id.dexc_DccexNextCommandButton);
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
//        final List<String> dccCvsValuesList = new ArrayList<>(Arrays.asList(dccCvsEntryValuesArray));
        dccCvsEntriesArray = this.getResources().getStringArray(R.array.dccCvsEntries); // display version
//        final List<String> dccCvsEntriesList = new ArrayList<>(Arrays.asList(dccCvsEntriesArray));

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
        dccexCommonCommandsSpinner = findViewById(R.id.dexc_common_commands_list);
        spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExCommonCommandsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccexCommonCommandsSpinner.setAdapter(spinner_adapter);
        dccexCommonCommandsSpinner.setOnItemSelectedListener(new DccExCommonCommandsSpinnerListener());
        dccexCommonCommandsSpinner.setSelection(dccCmdIndex);

        vn = 4;
        try {
            vn = Float.parseFloat(mainapp.DccexVersion);
        } catch (Exception ignored) { } // invalid version

        if (vn <= 04.002007) {  // need to remove the track manager option
            dccexActionTypeEntryValuesArray = new String[2];
            dccexActionTypeEntriesArray = new String[2];
            for (int i = 0; i < 2; i++) {
                dccexActionTypeEntryValuesArray[i] = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues)[i];
                dccexActionTypeEntriesArray[i] = this.getResources().getStringArray(R.array.dccExActionTypeEntries)[i];
            }
        } else {
            dccexActionTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues);
            dccexActionTypeEntriesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntries); // display version
        }
        final List<String> dccActionTypeValuesList = new ArrayList<>(Arrays.asList(dccexActionTypeEntryValuesArray));
        final List<String> dccActionTypeEntriesList = new ArrayList<>(Arrays.asList(dccexActionTypeEntriesArray));

//        mainapp.dccexActionTypeIndex = PROGRAMMING_TRACK;
        dccActionTypeSpinner = findViewById(R.id.dexc_action_type_list);
//        ArrayAdapter<?> dccActionTypeSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.dccExActionTypeEntries, android.R.layout.simple_spinner_item);
        ArrayAdapter<?> dccActionTypeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dccexActionTypeEntriesArray);
        dccActionTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccActionTypeSpinner.setAdapter(dccActionTypeSpinnerAdapter);
        dccActionTypeSpinner.setOnItemSelectedListener(new DccActionTypeSpinnerListener());
        dccActionTypeSpinner.setSelection(mainapp.dccexActionTypeIndex);

        LinearLayout cv_programmer_layout = findViewById(R.id.dccex_cv_programmer_prog_track_layout);
        DccExNavigationButtonListener dccExNavigationButtonListener = new DccExNavigationButtonListener(0);
        cv_programmer_layout.setOnClickListener(dccExNavigationButtonListener);
        Button cv_programmer_button = findViewById(R.id.dccex_cv_programmer_prog_track_button);
        dccExNavigationButtonListener = new DccExNavigationButtonListener(0);
        cv_programmer_button.setOnClickListener(dccExNavigationButtonListener);

        cv_programmer_layout = findViewById(R.id.dccex_cv_programmer_pom_layout);
        dccExNavigationButtonListener = new DccExNavigationButtonListener(1);
        cv_programmer_layout.setOnClickListener(dccExNavigationButtonListener);
        cv_programmer_button = findViewById(R.id.dccex_cv_programmer_pom_button);
        dccExNavigationButtonListener = new DccExNavigationButtonListener(1);
        cv_programmer_button.setOnClickListener(dccExNavigationButtonListener);

        cv_programmer_layout = findViewById(R.id.dccex_track_manager_layout);
        dccExNavigationButtonListener = new DccExNavigationButtonListener(2);
        cv_programmer_layout.setOnClickListener(dccExNavigationButtonListener);
        cv_programmer_button = findViewById(R.id.dccex_track_manager_button);
        dccExNavigationButtonListener = new DccExNavigationButtonListener(2);
        cv_programmer_button.setOnClickListener(dccExNavigationButtonListener);

        LinearLayout default_functions_layout = findViewById(R.id.dccex_default_functions_layout);
        DccexDefaultFunctionsButtonListener dccexDefaultFunctionsButtonListener = new DccexDefaultFunctionsButtonListener(this);
        default_functions_layout.setOnClickListener(dccexDefaultFunctionsButtonListener);
        Button default_functions_button = findViewById(R.id.dccex_default_functions_button);
        dccexDefaultFunctionsButtonListener = new DccexDefaultFunctionsButtonListener(this);
        default_functions_button.setOnClickListener(dccexDefaultFunctionsButtonListener);

        dexcProgrammingCommonCvsLayout = findViewById(R.id.dexc_programmingCommonCvsLayout);
        dexcProgrammingAddressLayout = findViewById(R.id.dexc_programmingAddressLayout);
        dexcProgrammingCvLayout = findViewById(R.id.dexc_programmingCvLayout);
        dexcDccexTrackLinearLayout = findViewById(R.id.dexc_DccexTrackLinearLayout);

        dccexTrackTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntryValues);
//        final List<String> dccTrackTypeValuesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntryValuesArray));
        dccexTrackTypeEntriesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntries); // display version
        if (vn <= 04.002068) {  /// need to change the NONE to OFF in track manager
            dccexTrackTypeEntriesArray[0] = "OFF";
        }
//        final List<String> dccTrackTypeEntriesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntriesArray));

        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccexTrackTypeLayout[0] = findViewById(R.id.dexc_DccexTrack0layout);
                    dccexTrackPowerButton[0] = findViewById(R.id.dccex_power_control_button_0);
                    dccexTrackTypeSpinner[0] = findViewById(R.id.dexc_track_type_0_list);
                    dccexTrackTypeIdEditText[0] = findViewById(R.id.dexc_track_0_value);
                    break;
                case 1:
                    dccexTrackTypeLayout[1] = findViewById(R.id.dexc_DccexTrack1layout);
                    dccexTrackPowerButton[1] = findViewById(R.id.dccex_power_control_button_1);
                    dccexTrackTypeSpinner[1] = findViewById(R.id.dexc_track_type_1_list);
                    dccexTrackTypeIdEditText[1] = findViewById(R.id.dexc_track_1_value);
                    break;
                case 2:
                    dccexTrackTypeLayout[2] = findViewById(R.id.dexc_DccexTrack2layout);
                    dccexTrackPowerButton[2] = findViewById(R.id.dccex_power_control_button_2);
                    dccexTrackTypeSpinner[2] = findViewById(R.id.dexc_track_type_2_list);
                    dccexTrackTypeIdEditText[2] = findViewById(R.id.dexc_track_2_value);
                    break;
                case 3:
                    dccexTrackTypeLayout[3] = findViewById(R.id.dexc_DccexTrack3layout);
                    dccexTrackPowerButton[3] = findViewById(R.id.dccex_power_control_button_3);
                    dccexTrackTypeSpinner[3] = findViewById(R.id.dexc_track_type_3_list);
                    dccexTrackTypeIdEditText[3] = findViewById(R.id.dexc_track_3_value);
                    break;
                case 4:
                    dccexTrackTypeLayout[4] = findViewById(R.id.dexc_DccexTrack4layout);
                    dccexTrackPowerButton[4] = findViewById(R.id.dccex_power_control_button_4);
                    dccexTrackTypeSpinner[4] = findViewById(R.id.dexc_track_type_4_list);
                    dccexTrackTypeIdEditText[4] = findViewById(R.id.dexc_track_4_value);
                    break;
                case 5:
                    dccexTrackTypeLayout[5] = findViewById(R.id.dexc_DccexTrack5layout);
                    dccexTrackPowerButton[5] = findViewById(R.id.dccex_power_control_button_5);
                    dccexTrackTypeSpinner[5] = findViewById(R.id.dexc_track_type_5_list);
                    dccexTrackTypeIdEditText[5] = findViewById(R.id.dexc_track_5_value);
                    break;
                case 6:
                    dccexTrackTypeLayout[6] = findViewById(R.id.dexc_DccexTrack6layout);
                    dccexTrackPowerButton[6] = findViewById(R.id.dccex_power_control_button_6);
                    dccexTrackTypeSpinner[6] = findViewById(R.id.dexc_track_type_6_list);
                    dccexTrackTypeIdEditText[6] = findViewById(R.id.dexc_track_6_value);
                    break;
                case 7:
                    dccexTrackTypeLayout[7] = findViewById(R.id.dexc_DccexTrack7layout);
                    dccexTrackPowerButton[7] = findViewById(R.id.dccex_power_control_button_7);
                    dccexTrackTypeSpinner[7] = findViewById(R.id.dexc_track_type_7_list);
                    dccexTrackTypeIdEditText[7] = findViewById(R.id.dexc_track_7_value);
                    break;
            }
            ArrayAdapter<?> track_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExTrackTypeEntries, android.R.layout.simple_spinner_item);
            track_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dccexTrackTypeSpinner[i].setAdapter(track_type_spinner_adapter);
            dccexTrackTypeSpinner[i].setOnItemSelectedListener(new TrackTypeCommonCvsSpinnerListenerr(dccexTrackTypeSpinner[i], i));
            dccexTrackTypeSpinner[i].setSelection(dccexTrackTypeIndex[i]);

            SetTrackPowerButtonListener  buttonListener = new SetTrackPowerButtonListener(i);
            dccexTrackPowerButton[i].setOnClickListener(buttonListener);
        }

        writeTracksButton = findViewById(R.id.dexc_DccexWriteTracksButton);
        WriteTracksButtonListener writeTracksButtonListener = new WriteTracksButtonListener();
        writeTracksButton.setOnClickListener(writeTracksButtonListener);

        joinTracksButton = findViewById(R.id.dexc_DccexJoinTracksButton);
        JoinTracksButtonListener joinTracksButtonListener = new JoinTracksButtonListener();
        joinTracksButton.setOnClickListener(joinTracksButtonListener);

//        ScrollView dccexResponsesScrollView = findViewById(R.id.dexc_DccexResponsesScrollView);
//        ScrollView dccexSendsScrollView = findViewById(R.id.dexc_DccexSendsScrollView);

        clearCommandsButton = findViewById(R.id.dexc_DCCEXclearCommandsButton);
        ClearCommandsButtonListener clearCommandsButtonListener = new ClearCommandsButtonListener();
        clearCommandsButton.setOnClickListener(clearCommandsButtonListener);

//            hideSendsButton = findViewById(R.id.dexc_DccexHideSendsButton);
//            hide_sends_button_listener hideSendsClickListener = new hide_sends_button_listener();
//            hideSendsButton.setOnClickListener(hideSendsClickListener);


        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

        LinearLayout screenNameLine = findViewById(R.id.screen_name_line);
        Toolbar toolbar = findViewById(R.id.toolbar);
        LinearLayout statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_dcc_ex),
                    "");
        }

    } // end onCreate

    // ************************************************************************************************************* //

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) { //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        activateJoinedButton(mainapp.dccexJoined);

        if (menu != null) {
            mainapp.displayEStop(menu);
            mainapp.displayFlashlightMenuButton(menu);
            mainapp.setFlashlightButton(menu);
            mainapp.displayPowerStateMenuButton(menu);
            mainapp.setPowerStateButton(menu);
        }
        //update power state
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
            Log.d("Engine_Driver", "onDestroy: mainapp.dcc_ex_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu myMenu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dcc_ex_menu, myMenu);
        menu = myMenu;
        mainapp.displayEStop(myMenu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);

        return super.onCreateOptionsMenu(menu);
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        if (key == KeyEvent.KEYCODE_BACK) {
            mainapp.dccexScreenIsOpen = false;
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    private void disconnect() {
        this.finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public class CloseButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            finish();
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
//        InputDevice idev = getDevice(event.getDeviceId());
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
        if (item.getItemId() == R.id.EmerStop) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlight(this, menu);
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(menu);
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
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
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
                etDccexSendCommandValue.setText(dccexSendCommandValue);
                etDccexSendCommandValue.requestFocus();
                etDccexSendCommandValue.setSelection(dccexSendCommandValue.length());
            }
            dccCmdIndex = 0;
            dccexCommonCommandsSpinner.setSelection(dccCmdIndex);
            dccexInfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
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
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            dccCvsIndex = 0;
            dccexCommonCvsSpinner.setSelection(dccCvsIndex);

            refreshDccexView();
            refreshDccexTracksView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class TrackTypeCommonCvsSpinnerListenerr implements AdapterView.OnItemSelectedListener {
        final Spinner mySpinner;
        final int myIndex;

        TrackTypeCommonCvsSpinnerListenerr(Spinner spinner, int index) {
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
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the soft keyboard to close
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

                // bit 4 is Railcom

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
                Log.e("EX_Toolbox", "Error processing cv29: " + e.getMessage());
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
        Drawable img = getResources().getDrawable(outValue.resourceId);
        btn.setBackground(img);
    }

    public class DccExNavigationButtonListener implements View.OnClickListener {
        final int myIndex;

        DccExNavigationButtonListener(int index) {
            myIndex = index;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
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
}
