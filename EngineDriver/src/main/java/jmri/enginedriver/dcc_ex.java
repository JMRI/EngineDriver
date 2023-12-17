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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.ScrollView;
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

    private String DccexCv = "";
    private String DccexCvValue = "";
    private EditText etDccexCv;
    private EditText etDccexCvValue;

    private String DccexAddress = "";
    private EditText etDccexWriteAddressValue;

    private String dccexSendCommandValue = "";
    private EditText etDccexSendCommandValue;

    private LinearLayout DccexWriteInfoLayout;
    private TextView DccexWriteInfoLabel;
    private String DccexInfoStr = "";

    private TextView DccexResponsesLabel;
    private TextView DccexSendsLabel;
    private String DccexResponsesStr = "";
    private String DccexSendsStr = "";
    private ScrollView DccexResponsesScrollView;
    private ScrollView DccexSendsScrollView;

    ArrayList<String> DccexResponsesListHtml = new ArrayList<>();
    ArrayList<String> DccexSendsListHtml = new ArrayList<>();

    private int dccCvsIndex = 0;
    String[] dccCvsEntryValuesArray;
    String[] dccCvsEntriesArray; // display version

    private int dccCmdIndex = 0;
    String[] dccExCommonCommandsEntryValuesArray;
    String[] dccExCommonCommandsEntriesArray; // display version
    int[] dccExCommonCommandsHasParametersArray; // display version

    private int dccExActionTypeIndex = 0;
    String[] dccExActionTypeEntryValuesArray;
    String[] dccExActionTypeEntriesArray; // display version

//    private boolean dccexHideSends = false;

    private static final int PROGRAMMING_TRACK = 0;
    private static final int PROGRAMMING_ON_MAIN = 1;
    private static final int TRACK_MANAGER = 2;

    Button readAddressButton;
    Button writeAddressButton;
    Button readCvButton;
    Button writeCvButton;
    Button sendCommandButton;
    Button previousCommandButton;
    Button nextCommandButton;
    Button writeTracksButton;
    //    Button hideSendsButton;
    Button clearCommandsButton;

    private LinearLayout dexcProgrammingCommonCvsLayout;
    private LinearLayout dexcProgrammingAddressLayout;
    private LinearLayout dexcProgrammingCvLayout;
    private LinearLayout[] dexcDccexTracklayout = {null, null, null, null, null, null, null, null};
    private LinearLayout dexcDccexTrackLinearLayout;
    Spinner dccExCommonCvsSpinner;
    Spinner dccExCommonCommandsSpinner;

    private int[] dccExTrackTypeIndex = {1, 2, 1, 1, 1, 1, 1, 1};
    private Button[] dccExTrackPowerButton = {null, null, null, null, null, null, null, null};
    private Spinner[] dccExTrackTypeSpinner = {null, null, null, null, null, null, null, null};
    private EditText[] dccExTrackTypeIdEditText = {null, null, null, null, null, null, null, null};
    private LinearLayout[] dccExTrackTypeLayout = {null, null, null, null, null, null, null, null};

    String[] dccExTrackTypeEntryValuesArray;
    String[] dccExTrackTypeEntriesArray; // display version

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

    static final String[] TRACK_TYPES = {"NONE", "MAIN", "PROG", "DC", "DCX"};
    static final boolean[] TRACK_TYPES_NEED_ID = {false, false, false, true, true};

    String cv29SpeedSteps;
    String cv29AnalogueMode;
    String cv29Direction;
    String cv29AddressSize;
    String cv29SpeedTable;

    float vn = 4; // DCC-EC Version number

    //**************************************


    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class dcc_ex_handler extends Handler {

        public dcc_ex_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RECEIVED_DECODER_ADDRESS:
                    String response_str = msg.obj.toString();
                    if ((response_str.length() > 0) && !(response_str.charAt(0) == '-')) {  //refresh address
                        DccexAddress = response_str;
                        DccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexSucceeded);
                    } else {
                        DccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexFailed);
                    }
                    refreshDccexView();
                    break;
                case message_type.RECEIVED_CV:
                    String cvResponseStr = msg.obj.toString();
                    if (cvResponseStr.length() > 0) {
                        String[] cvArgs = cvResponseStr.split("(\\|)");
                        if ((cvArgs[0].equals(DccexCv)) && !(cvArgs[1].charAt(0) == '-')) { // response matches what we got back
                            DccexCvValue = cvArgs[1];
                            DccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexSucceeded);
                            checkCv29(DccexCv, DccexCvValue);
                        } else {
                            resetTextField(WHICH_CV_VALUE);
                            DccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexFailed);
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
                    DccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexSucceeded);
                    refreshDccexView();
                    break;
                case message_type.WRITE_DECODER_FAIL:
                    DccexInfoStr = getApplicationContext().getResources().getString(R.string.DccexFailed);
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

    public class read_address_button_listener implements View.OnClickListener {

        public void onClick(View v) {
            DccexInfoStr = "";
            resetTextField(WHICH_ADDRESS);
            mainapp.buttonVibration();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_DECODER_ADDRESS, "*", -1);
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class write_address_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            String addrStr = etDccexWriteAddressValue.getText().toString();
            try {
                Integer addr = Integer.decode(addrStr);
                if ((addr > 2) && (addr <= 10239)) {
                    DccexAddress = addr.toString();
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

    public class read_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            resetTextField(WHICH_CV_VALUE);
            String cvStr = etDccexCv.getText().toString();
            try {
                int cv = Integer.decode(cvStr);
                if (cv > 0) {
                    DccexCv = Integer.toString(cv);
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

    public class write_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            String cvStr = etDccexCv.getText().toString();
            String cvValueStr = etDccexCvValue.getText().toString();
            String addrStr = etDccexWriteAddressValue.getText().toString();
            if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
                try {
                    Integer cv = Integer.decode(cvStr);
                    int cvValue = Integer.decode(cvValueStr);
                    if (cv > 0) {
                        DccexCv = cv.toString();
                        DccexCvValue = Integer.toString(cvValue);
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
                    Integer addr = Integer.decode(addrStr);
                    if ((addr > 2) && (addr <= 10239) && (cv > 0)) {
                        DccexAddress = addr.toString();
                        DccexCv = cv.toString();
                        DccexCvValue = Integer.toString(cvValue);
                        mainapp.buttonVibration();
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_POM_CV, DccexCv + " " + DccexCvValue, addr);
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

    public class send_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            String cmdStr = etDccexSendCommandValue.getText().toString();
            if ((cmdStr.length() > 0) && (cmdStr.charAt(0) != '<')) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_SEND_COMMAND, "<" + cmdStr + ">");

                if ((cmdStr.charAt(0) == '=') && (cmdStr.length() > 1)) // we don't get a response from a tracks command, so request an update
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

                if ((mainapp.dccexPreviousCommandList.size() == 0) || !(mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandList.size() - 1).equals(cmdStr))) {
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

    public class previous_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
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

    public class next_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
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
        int myTrack;
        char myTrackLetter;

        public SetTrackPowerButtonListener(int track) {
            myTrack = track;
            myTrackLetter = (char) ('A' + track);
        }

        public void onClick(View v) {
            if (mainapp.DccexTrackPower[myTrack] == 0 ) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK_POWER, ""+myTrackLetter, 1);
            } else {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK_POWER, ""+myTrackLetter, 0);
            }
        }
    }

    public class write_tracks_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            Integer typeIndex;
            String type;
            Integer id;
            char trackLetter;

            for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
                if (mainapp.DccexTrackAvailable[i]) {
                    trackLetter = (char) ('A' + i);
                    typeIndex = dccExTrackTypeSpinner[i].getSelectedItemPosition();
                    type = TRACK_TYPES[typeIndex];
                    mainapp.DccexTrackType[i] = typeIndex;

                    if (!TRACK_TYPES_NEED_ID[typeIndex]) {
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, 0);
                    } else {
                        try {
                            id = Integer.parseInt(dccExTrackTypeIdEditText[i].getText().toString());
                            mainapp.DccexTrackId[i] = id.toString();
                            if (mainapp.DccexTrackType[i] != TRACK_TYPE_OFF_NONE_INDEX) {
                                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, id);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");
//            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS_POWER, "");
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class clear_commands_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexResponsesListHtml.clear();
            DccexSendsListHtml.clear();
            DccexResponsesStr = "";
            DccexSendsStr = "";
            refreshDccexView();
        }
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
                DccexAddress = "";
                etDccexWriteAddressValue.setText("");
                break;
            case WHICH_CV:
                DccexCv = "";
                etDccexCv.setText("");
                break;
            case WHICH_CV_VALUE:
                DccexCvValue = "";
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
                DccexAddress = etDccexWriteAddressValue.getText().toString();
                break;
            case WHICH_CV:
                DccexCv = etDccexCv.getText().toString();
                break;
            case WHICH_CV_VALUE:
                DccexCvValue = etDccexCvValue.getText().toString();
                break;
            case WHICH_COMMAND:
                dccexSendCommandValue = etDccexSendCommandValue.getText().toString();
        }
    }

    private void showHideButtons() {
        if (dccExActionTypeIndex != TRACK_MANAGER) {
            dexcProgrammingCommonCvsLayout.setVisibility(View.VISIBLE);
            dccExCommonCvsSpinner.setVisibility(View.VISIBLE);

            dexcProgrammingAddressLayout.setVisibility(View.VISIBLE);
            dexcProgrammingCvLayout.setVisibility(View.VISIBLE);
            dexcDccexTrackLinearLayout.setVisibility(View.GONE);
            DccexWriteInfoLayout.setVisibility(View.VISIBLE);

            sendCommandButton.setEnabled(false);
            writeAddressButton.setEnabled(DccexAddress.length() != 0);
            readCvButton.setEnabled(DccexCv.length() != 0);
            if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
                writeCvButton.setEnabled(((DccexCv.length() != 0) && (DccexCvValue.length() != 0)));
            } else {
                writeCvButton.setEnabled(((DccexCv.length() != 0) && (DccexCvValue.length() != 0) && (DccexAddress.length() != 0)));
            }
        } else {
            dexcProgrammingCommonCvsLayout.setVisibility(View.GONE);
            dccExCommonCvsSpinner.setVisibility(View.GONE);

            dexcProgrammingAddressLayout.setVisibility(View.GONE);
            dexcProgrammingCvLayout.setVisibility(View.GONE);
            DccexWriteInfoLayout.setVisibility(View.GONE);
            dexcDccexTrackLinearLayout.setVisibility(View.VISIBLE);

            for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
                dccExTrackTypeIdEditText[i].setVisibility(TRACK_TYPES_NEED_ID[dccExTrackTypeIndex[i]] ? View.VISIBLE : View.GONE);
            }
        }
        sendCommandButton.setEnabled((dccexSendCommandValue.length() != 0) && (dccexSendCommandValue.charAt(0) != '<'));
        previousCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
        nextCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
    }

    public void refreshDccexView() {

        etDccexWriteAddressValue.setText(DccexAddress);
        DccexWriteInfoLabel.setText(DccexInfoStr);
        etDccexCv.setText(DccexCv);
        etDccexCvValue.setText(DccexCvValue);
//        etDccexSendCommandValue.setText(dccexSendCommandValue);

        if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
            readAddressButton.setVisibility(View.VISIBLE);
            writeAddressButton.setVisibility(View.VISIBLE);
            readCvButton.setVisibility(View.VISIBLE);
        } else {
            readAddressButton.setVisibility(View.GONE);
            writeAddressButton.setVisibility(View.GONE);
            readCvButton.setVisibility(View.GONE);
        }

        refreshDccexCommandsView();

        showHideButtons();

        if (menu != null) {
            mainapp.displayEStop(menu);
        }
    }

    public void refreshDccexCommandsView() {
        DccexResponsesLabel.setText(Html.fromHtml(DccexResponsesStr));
        DccexSendsLabel.setText(Html.fromHtml(DccexSendsStr));
    }

    public void refreshDccexTracksView() {

        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            dccExTrackTypeSpinner[i].setSelection(mainapp.DccexTrackType[i]);
            dccExTrackTypeIdEditText[i].setText(mainapp.DccexTrackId[i]);
            dccExTrackTypeLayout[i].setVisibility(mainapp.DccexTrackAvailable[i] ? View.VISIBLE : View.GONE);
            if (vn >= 5.002005) {
                setPowerbutton(dccExTrackPowerButton[i], mainapp.DccexTrackPower[i]);
            } else {
                dccExTrackPowerButton[i].setVisibility(View.GONE);
            }
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
            DccexResponsesListHtml.add("<small><small>" + currentTime + " </small></small> ◄ : <b>" + Html.escapeHtml(msg) + "</b><br />");
        } else {
//            DccexSendsListHtml.add("<small><small>" + currentTime + " </small></small> ► : &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp <i>" + Html.escapeHtml(msg) + "</i><br />");
            DccexSendsListHtml.add("<small><small>" + currentTime + " </small></small> ► : <i>" + Html.escapeHtml(msg) + "</i><br />");
        }
        if (DccexResponsesListHtml.size() > 40) {
            DccexResponsesListHtml.remove(0);
        }
        if (DccexSendsListHtml.size() > 30) {
            DccexSendsListHtml.remove(0);
        }

        DccexResponsesStr = "<p>";
        for (int i = 0; i < DccexResponsesListHtml.size(); i++) {
            DccexResponsesStr = DccexResponsesListHtml.get(i) + DccexResponsesStr;
        }
        DccexResponsesStr = DccexResponsesStr + "</p>";

        DccexSendsStr = "<p>";
        for (int i=0; i < DccexSendsListHtml.size(); i++) {
            DccexSendsStr = DccexSendsListHtml.get(i) + DccexSendsStr;
        }
        DccexSendsStr = DccexSendsStr + "</p>";
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
        mainapp.dcc_ex_msg_handler = new dcc_ex_handler(Looper.getMainLooper());

        readAddressButton = findViewById(R.id.dexc_DccexReadAddressButton);
        read_address_button_listener read_address_click_listener = new read_address_button_listener();
        readAddressButton.setOnClickListener(read_address_click_listener);

        writeAddressButton = findViewById(R.id.dexc_DccexWriteAddressButton);
        write_address_button_listener write_address_click_listener = new write_address_button_listener();
        writeAddressButton.setOnClickListener(write_address_click_listener);

        etDccexWriteAddressValue = findViewById(R.id.dexc_DccexWriteAddressValue);
        etDccexWriteAddressValue.setText("");
        etDccexWriteAddressValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_ADDRESS); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        readCvButton = findViewById(R.id.dexc_DccexReadCvButton);
        read_cv_button_listener readCvClickListener = new read_cv_button_listener();
        readCvButton.setOnClickListener(readCvClickListener);

        writeCvButton = findViewById(R.id.dexc_DccexWriteCvButton);
        write_cv_button_listener writeCvClickListener = new write_cv_button_listener();
        writeCvButton.setOnClickListener(writeCvClickListener);

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
        send_command_button_listener sendCommandClickListener = new send_command_button_listener();
        sendCommandButton.setOnClickListener(sendCommandClickListener);

        etDccexSendCommandValue = findViewById(R.id.dexc_DccexSendCommandValue);
        etDccexSendCommandValue.setInputType(TYPE_TEXT_FLAG_AUTO_CORRECT);
        etDccexSendCommandValue.setText("");
        etDccexSendCommandValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_COMMAND); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        DccexWriteInfoLayout = findViewById(R.id.dexc_DccexWriteInfoLayout);
        DccexWriteInfoLabel = findViewById(R.id.dexc_DccexWriteInfoLabel);
        DccexWriteInfoLabel.setText("");

        previousCommandButton = findViewById(R.id.dexc_DccexPreviousCommandButton);
        previous_command_button_listener previousCommandClickListener = new previous_command_button_listener();
        previousCommandButton.setOnClickListener(previousCommandClickListener);

        nextCommandButton = findViewById(R.id.dexc_DccexNextCommandButton);
        next_command_button_listener nextCommandClickListener = new next_command_button_listener();
        nextCommandButton.setOnClickListener(nextCommandClickListener);

        DccexResponsesLabel = findViewById(R.id.dexc_DccexResponsesLabel);
        DccexResponsesLabel.setText("");
        DccexSendsLabel = findViewById(R.id.dexc_DccexSendsLabel);
        DccexSendsLabel.setText("");

        Button closeButton = findViewById(R.id.dcc_ex_button_close);
        close_button_listener closeClickListener = new close_button_listener();
        closeButton.setOnClickListener(closeClickListener);

        dccCvsEntryValuesArray = this.getResources().getStringArray(R.array.dccCvsEntryValues);
//        final List<String> dccCvsValuesList = new ArrayList<>(Arrays.asList(dccCvsEntryValuesArray));
        dccCvsEntriesArray = this.getResources().getStringArray(R.array.dccCvsEntries); // display version
//        final List<String> dccCvsEntriesList = new ArrayList<>(Arrays.asList(dccCvsEntriesArray));

        dccCvsIndex = 0;
        dccExCommonCvsSpinner = findViewById(R.id.dexc_dcc_cv_list);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccCvsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExCommonCvsSpinner.setAdapter(spinner_adapter);
        dccExCommonCvsSpinner.setOnItemSelectedListener(new spinner_listener());
        dccExCommonCvsSpinner.setSelection(dccCvsIndex);

        dccExCommonCommandsEntryValuesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntryValues);
//        final List<String> dccCommonCommandsValuesList = new ArrayList<>(Arrays.asList(dccExCommonCommandsEntryValuesArray));
        dccExCommonCommandsEntriesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntries); // display version
//        final List<String> dccCommonCommandsEntriesList = new ArrayList<>(Arrays.asList(dccExCommonCommandsEntriesArray));
        dccExCommonCommandsHasParametersArray = this.getResources().getIntArray(R.array.dccExCommonCommandsHasParameters);

        dccCmdIndex = 0;
        dccExCommonCommandsSpinner = findViewById(R.id.dexc_common_commands_list);
        spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExCommonCommandsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExCommonCommandsSpinner.setAdapter(spinner_adapter);
        dccExCommonCommandsSpinner.setOnItemSelectedListener(new command_spinner_listener());
        dccExCommonCommandsSpinner.setSelection(dccCmdIndex);

        vn = 4;
        try {
            vn = Float.valueOf(mainapp.DccexVersion);
        } catch (Exception ignored) { } // invalid version

        if (vn <= 04.002007) {  // need to remove the track manager option
            dccExActionTypeEntryValuesArray = new String[2];
            dccExActionTypeEntriesArray = new String[2];
            for (int i = 0; i < 2; i++) {
                dccExActionTypeEntryValuesArray[i] = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues)[i];
                dccExActionTypeEntriesArray[i] = this.getResources().getStringArray(R.array.dccExActionTypeEntries)[i];
            }
        } else {
            dccExActionTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues);
            dccExActionTypeEntriesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntries); // display version
        }
        final List<String> dccActionTypeValuesList = new ArrayList<>(Arrays.asList(dccExActionTypeEntryValuesArray));
        final List<String> dccActionTypeEntriesList = new ArrayList<>(Arrays.asList(dccExActionTypeEntriesArray));

        dccExActionTypeIndex = PROGRAMMING_TRACK;
        Spinner dcc_action_type_spinner = findViewById(R.id.dexc_action_type_list);
//        ArrayAdapter<?> action_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExActionTypeEntries, android.R.layout.simple_spinner_item);
        ArrayAdapter<?> action_type_spinner_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dccExActionTypeEntriesArray);
        action_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dcc_action_type_spinner.setAdapter(action_type_spinner_adapter);
        dcc_action_type_spinner.setOnItemSelectedListener(new action_type_spinner_listener());
        dcc_action_type_spinner.setSelection(dccExActionTypeIndex);

        dexcProgrammingCommonCvsLayout = findViewById(R.id.dexc_programmingCommonCvsLayout);
        dexcProgrammingAddressLayout = findViewById(R.id.dexc_programmingAddressLayout);
        dexcProgrammingCvLayout = findViewById(R.id.dexc_programmingCvLayout);
        dexcDccexTrackLinearLayout = findViewById(R.id.dexc_DccexTrackLinearLayout);

        dccExTrackTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntryValues);
//        final List<String> dccTrackTypeValuesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntryValuesArray));
        dccExTrackTypeEntriesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntries); // display version
        if (vn <= 04.002068) {  /// need to change the NONE to OFF in track manager
            dccExTrackTypeEntriesArray[0] = "OFF";
        }
//        final List<String> dccTrackTypeEntriesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntriesArray));

        for (int i=0; i < mainapp.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccExTrackTypeLayout[0] = findViewById(R.id.dexc_DccexTrack0layout);
                    dccExTrackPowerButton[0] = findViewById(R.id.dccex_power_control_button_0);
                    dccExTrackTypeSpinner[0] = findViewById(R.id.dexc_track_type_0_list);
                    dccExTrackTypeIdEditText[0] = findViewById(R.id.dexc_track_0_value);
                    break;
                case 1:
                    dccExTrackTypeLayout[1] = findViewById(R.id.dexc_DccexTrack1layout);
                    dccExTrackPowerButton[1] = findViewById(R.id.dccex_power_control_button_1);
                    dccExTrackTypeSpinner[1] = findViewById(R.id.dexc_track_type_1_list);
                    dccExTrackTypeIdEditText[1] = findViewById(R.id.dexc_track_1_value);
                    break;
                case 2:
                    dccExTrackTypeLayout[2] = findViewById(R.id.dexc_DccexTrack2layout);
                    dccExTrackPowerButton[2] = findViewById(R.id.dccex_power_control_button_2);
                    dccExTrackTypeSpinner[2] = findViewById(R.id.dexc_track_type_2_list);
                    dccExTrackTypeIdEditText[2] = findViewById(R.id.dexc_track_2_value);
                    break;
                case 3:
                    dccExTrackTypeLayout[3] = findViewById(R.id.dexc_DccexTrack3layout);
                    dccExTrackPowerButton[3] = findViewById(R.id.dccex_power_control_button_3);
                    dccExTrackTypeSpinner[3] = findViewById(R.id.dexc_track_type_3_list);
                    dccExTrackTypeIdEditText[3] = findViewById(R.id.dexc_track_3_value);
                    break;
                case 4:
                    dccExTrackTypeLayout[4] = findViewById(R.id.dexc_DccexTrack4layout);
                    dccExTrackPowerButton[4] = findViewById(R.id.dccex_power_control_button_4);
                    dccExTrackTypeSpinner[4] = findViewById(R.id.dexc_track_type_4_list);
                    dccExTrackTypeIdEditText[4] = findViewById(R.id.dexc_track_4_value);
                    break;
                case 5:
                    dccExTrackTypeLayout[5] = findViewById(R.id.dexc_DccexTrack5layout);
                    dccExTrackPowerButton[5] = findViewById(R.id.dccex_power_control_button_5);
                    dccExTrackTypeSpinner[5] = findViewById(R.id.dexc_track_type_5_list);
                    dccExTrackTypeIdEditText[5] = findViewById(R.id.dexc_track_5_value);
                    break;
                case 6:
                    dccExTrackTypeLayout[6] = findViewById(R.id.dexc_DccexTrack6layout);
                    dccExTrackPowerButton[6] = findViewById(R.id.dccex_power_control_button_6);
                    dccExTrackTypeSpinner[6] = findViewById(R.id.dexc_track_type_6_list);
                    dccExTrackTypeIdEditText[6] = findViewById(R.id.dexc_track_6_value);
                    break;
                case 7:
                    dccExTrackTypeLayout[7] = findViewById(R.id.dexc_DccexTrack7layout);
                    dccExTrackPowerButton[7] = findViewById(R.id.dccex_power_control_button_7);
                    dccExTrackTypeSpinner[7] = findViewById(R.id.dexc_track_type_7_list);
                    dccExTrackTypeIdEditText[7] = findViewById(R.id.dexc_track_7_value);
                    break;
            }
            ArrayAdapter<?> track_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExTrackTypeEntries, android.R.layout.simple_spinner_item);
            track_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dccExTrackTypeSpinner[i].setAdapter(track_type_spinner_adapter);
            dccExTrackTypeSpinner[i].setOnItemSelectedListener(new track_type_spinner_listener(dccExTrackTypeSpinner[i], i));
            dccExTrackTypeSpinner[i].setSelection(dccExTrackTypeIndex[i]);

            SetTrackPowerButtonListener  buttonListener = new SetTrackPowerButtonListener(i);
            dccExTrackPowerButton[i].setOnClickListener(buttonListener);
        }

        writeTracksButton = findViewById(R.id.dexc_DccexWriteTracksButton);
        write_tracks_button_listener writeTracksClickListener = new write_tracks_button_listener();
        writeTracksButton.setOnClickListener(writeTracksClickListener);

        DccexResponsesScrollView = findViewById(R.id.dexc_DccexResponsesScrollView);
        DccexSendsScrollView = findViewById(R.id.dexc_DccexSendsScrollView);

        clearCommandsButton = findViewById(R.id.dexc_DCCEXclearCommandsButton);
        clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);

//            hideSendsButton = findViewById(R.id.dexc_DccexHideSendsButton);
//            hide_sends_button_listener hideSendsClickListener = new hide_sends_button_listener();
//            hideSendsButton.setOnClickListener(hideSendsClickListener);


        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            mainapp.setToolbarTitle(toolbar,
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

    public class close_button_listener implements View.OnClickListener {
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
        } else if (item.getItemId() == R.id.power_layout_button) {
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

    public class spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            dccCvsIndex = dccExCommonCvsSpinner.getSelectedItemPosition();
            if (dccCvsIndex > 0) {
                DccexCv = dccCvsEntryValuesArray[dccCvsIndex];
                resetTextField(WHICH_CV_VALUE);
                etDccexCvValue.requestFocus();
            }
            dccCvsIndex = 0;
            dccExCommonCvsSpinner.setSelection(dccCvsIndex);
            DccexInfoStr = "";

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

    public class command_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            dccCmdIndex = dccExCommonCommandsSpinner.getSelectedItemPosition();
            if (dccCmdIndex > 0) {
                dccexSendCommandValue = dccExCommonCommandsEntryValuesArray[dccCmdIndex];
                if (dccExCommonCommandsHasParametersArray[dccCmdIndex] > 0)
                    dccexSendCommandValue = dccexSendCommandValue + " ";
                etDccexSendCommandValue.setText(dccexSendCommandValue);
                etDccexSendCommandValue.requestFocus();
                etDccexSendCommandValue.setSelection(dccexSendCommandValue.length());
            }
            dccCmdIndex = 0;
            dccExCommonCommandsSpinner.setSelection(dccCmdIndex);
            DccexInfoStr = "";

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

    public class action_type_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            Spinner spinner = findViewById(R.id.dexc_action_type_list);
            dccExActionTypeIndex = spinner.getSelectedItemPosition();
            resetTextField(WHICH_CV);
            resetTextField(WHICH_CV_VALUE);
            DccexInfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            dccCvsIndex = 0;
            dccExCommonCvsSpinner.setSelection(dccCvsIndex);

            refreshDccexView();
            refreshDccexTracksView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class track_type_spinner_listener implements AdapterView.OnItemSelectedListener {
        Spinner mySpinner;
        int myIndex;

        track_type_spinner_listener(Spinner spinner, int index) {
            mySpinner = spinner;
            myIndex = index;
        }

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            dccExTrackTypeIndex[myIndex] = mySpinner.getSelectedItemPosition();
            if (dccExTrackTypeIndex[myIndex] == TRACK_TYPE_DCC_PROG_INDEX) {
                for (int i=0; i < 8; i++) {
                    if ((dccExTrackTypeIndex[i] == TRACK_TYPE_DCC_PROG_INDEX) && (myIndex != i)) { // only one prog allowed
                        dccExTrackTypeSpinner[i].setSelection(TRACK_TYPE_OFF_NONE_INDEX);
                    }
                }
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

                DccexResponsesStr = "<p>" + rslt + "</p>" + DccexResponsesStr;

                DccexResponsesStr = "<p>"
                        + String.format(getApplicationContext().getResources().getString(R.string.cv29SpeedToggleDirection),
                        mainapp.toggleBit(cvValue, 1))
                        + "</p>" + DccexResponsesStr;

            } catch (Exception e) {
                Log.e("EX_Toolbox", "Error processign cv29: " + e.getMessage());
            }
        }
    }

    void setPowerbutton(Button btn, int powerState) {
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
}
