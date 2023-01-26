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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
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
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class dcc_ex extends AppCompatActivity {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu menu;

    private String DCCEXcv = "";
    private String DCCEXcvValue = "";
    private EditText etDCCEXcv;
    private EditText etDCCEXcvValue;

    private String DCCEXaddress = "";
    private EditText etDCCEXwriteAddressValue;

    private String DCCEXsendCommandValue = "";
    private EditText etDCCEXsendCommandValue;

    private TextView DCCEXwriteInfoLabel;
    private String DCCEXinfoStr = "";

    private TextView DCCEXresponsesLabel;
    private String DCCEXresponsesStr = "";

    ArrayList<String> DCCEXresponsesListHtml = new ArrayList<>();

    private int dccCvsIndex = 0;
    String[] dccCvsEntryValuesArray;
    String[] dccCvsEntriesArray; // display version

    private int dccExActionTypeIndex = 0;
    String[] dccExActionTypeEntryValuesArray;
    String[] dccExActionTypeEntriesArray; // display version


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

    private LinearLayout dexcProgrammingCommonCvsLayout;
    private LinearLayout dexcProgrammingAddressLayout;
    private LinearLayout dexcProgrammingCvLayout;
    private LinearLayout[] dexcDCCEXtracklayout = {null, null, null, null, null, null, null, null};
    private LinearLayout dexcDCCEXtrackLinearLayout;
    Spinner dccExCommonCvsSpinner;

    private int[] dccExTrackTypeIndex = {1, 2, 1, 1, 1, 1, 1, 1};
    private Spinner [] dccExTrackTypeSpinner = {null, null, null, null, null, null, null, null};
    private EditText [] dccExTrackTypeIdEditText = {null, null, null, null, null, null, null, null};
    private LinearLayout [] dccExTrackTypeLayout = {null, null, null, null, null, null, null, null};

    String[] dccExTrackTypeEntryValuesArray;
    String[] dccExTrackTypeEntriesArray; // display version

    private int DCCEXpreviousCommandIndex = -1;
    ArrayList<String> DCCEXpreviousCommandList = new ArrayList<>();

    static final int WHICH_ADDRESS = 0;
    static final int WHICH_CV = 1;
    static final int WHICH_CV_VALUE = 2;
    static final int WHICH_COMMAND = 3;

    static final int TRACK_TYPE_OFF_INDEX = 0;
    static final int TRACK_TYPE_DCC_MAIN_INDEX = 1;
    static final int TRACK_TYPE_DCC_PROG_INDEX = 2;
    static final int TRACK_TYPE_DC_INDEX = 3;
    static final int TRACK_TYPE_DCX_INDEX = 4;

    static final String [] TRACK_TYPES = { "OFF", "MAIN", "PROG", "DC", "DCX"};
    static final boolean [] TRACK_TYPES_NEED_ID = { false, false, false, true, true };

    private Toolbar toolbar;
    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class dcc_ex_handler extends Handler {


        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RECEIVED_DECODER_ADDRESS:
                    String response_str = msg.obj.toString();
                    if ((response_str.length() > 0) && (!response_str.equals("-1"))) {  //refresh address
                        DCCEXaddress = response_str;
                        DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXSucceeded);
                    } else {
                        DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXFailed);
                    }
                    refreshDCCEXview();
                    break;
                case message_type.RECEIVED_CV:
                    String cvResponseStr = msg.obj.toString();
                    if (cvResponseStr.length() > 0) {
                        String[] cvArgs = cvResponseStr.split("(\\|)");
                        if ((cvArgs[0].equals(DCCEXcv)) && (!cvArgs[1].equals("-1"))) { // response matches what we got back
                            DCCEXcvValue = cvArgs[1];
                            DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXSucceeded);
                        } else {
                            resetTextField(WHICH_CV_VALUE);
                            DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXFailed);
                        }
                        refreshDCCEXview();
                    }
                    break;

                case message_type.DCCEX_COMMAND_ECHO:  // informational response
                    displayCommands(msg.obj.toString(), false);
                    refreshDCCEXview();
                    break;

                case message_type.DCCEX_RESPONSE:  // informational response
                    displayCommands(msg.obj.toString(), true);
                    refreshDCCEXview();
                    break;

                case message_type.WRITE_DECODER_SUCCESS:
                    DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXSucceeded);
                    refreshDCCEXview();
                    break;
                case message_type.WRITE_DECODER_FAIL:
                    DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXFailed);
                    refreshDCCEXview();
                    break;
                case message_type.RECEIVED_TRACKS:
                    refreshDCCEXtracksView();
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refreshDCCEXview();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateButton(menu);
                        }
                    }
                    break;
                }
            }
        }
    }
    public class read_address_button_listener implements View.OnClickListener {

        public void onClick(View v) {
            DCCEXinfoStr = "";
            resetTextField(WHICH_ADDRESS);
            mainapp.buttonVibration();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_DECODER_ADDRESS, "*", -1);
            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);

        }
    }

    public class write_address_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String addrStr = etDCCEXwriteAddressValue.getText().toString();
            try {
                Integer addr = Integer.decode(addrStr);
                if ((addr>2) && (addr<=10239)) {
                    DCCEXaddress = addr.toString();
                    mainapp.buttonVibration();
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_DECODER_ADDRESS, "", addr);
                } else {
                    resetTextField(WHICH_ADDRESS);
                }
            } catch (Exception e) {
                resetTextField(WHICH_ADDRESS);
            }
            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);

        }
    }

    public class read_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            resetTextField(WHICH_CV_VALUE);
            String cvStr = etDCCEXcv.getText().toString();
            try {
                int cv = Integer.decode(cvStr);
                if (cv > 0) {
                    DCCEXcv = Integer.toString(cv);
                    mainapp.buttonVibration();
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CV, "", cv);
                    refreshDCCEXview();
                }
            } catch (Exception e) {
                resetTextField(WHICH_CV);
            }
            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class write_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cvStr = etDCCEXcv.getText().toString();
            String cvValueStr = etDCCEXcvValue.getText().toString();
            String addrStr = etDCCEXwriteAddressValue.getText().toString();
            if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
                try {
                    Integer cv = Integer.decode(cvStr);
                    int cvValue = Integer.decode(cvValueStr);
                    if ((cv > 0) && (cvValue > 0)) {
                        DCCEXcv = cv.toString();
                        DCCEXcvValue = Integer.toString(cvValue);
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
                    if ( (addr>2) && (addr<=9999) && (cv > 0) && (cvValue > 0) ) {
                        DCCEXaddress = addr.toString();
                        DCCEXcv = cv.toString();
                        DCCEXcvValue = Integer.toString(cvValue);
                        mainapp.buttonVibration();
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_POM_CV, DCCEXcv+" "+DCCEXcvValue, addr);
                    } else {
                        resetTextField(WHICH_ADDRESS);
                    }
                } catch (Exception e) {
                    resetTextField(WHICH_ADDRESS);
                }
            }
            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class send_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cmdStr = etDCCEXsendCommandValue.getText().toString();
            if ((cmdStr.length()>0) && (cmdStr.charAt(0)!='<')) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_SEND_COMMAND, "<"+cmdStr+">");

                if ((cmdStr.charAt(0)=='=') && (cmdStr.length()>1) ) // we don't get a response from a tracks command, so request an update
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

                if ( (DCCEXpreviousCommandList.size()<=0) || !(DCCEXpreviousCommandList.get(DCCEXpreviousCommandList.size()-1).equals(cmdStr)) ) {
                    DCCEXpreviousCommandList.add(cmdStr);
                    if (DCCEXpreviousCommandList.size() > 20) {
                        DCCEXpreviousCommandList.remove(0);
                    }
                }
                DCCEXpreviousCommandIndex = DCCEXpreviousCommandList.size();
            }
            resetTextField(WHICH_COMMAND);
            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class previous_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cmdStr = etDCCEXsendCommandValue.getText().toString();
            if (DCCEXpreviousCommandIndex>0) {
                DCCEXsendCommandValue = DCCEXpreviousCommandList.get(DCCEXpreviousCommandIndex-1);
                DCCEXpreviousCommandIndex--;
            } else {
                DCCEXsendCommandValue = DCCEXpreviousCommandList.get(DCCEXpreviousCommandList.size()-1);
                DCCEXpreviousCommandIndex = DCCEXpreviousCommandList.size() -1;
            }
            etDCCEXsendCommandValue.setText(DCCEXsendCommandValue);

            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class next_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cmdStr = etDCCEXsendCommandValue.getText().toString();
            if (DCCEXpreviousCommandIndex<DCCEXpreviousCommandList.size()-1) {
                DCCEXsendCommandValue = DCCEXpreviousCommandList.get(DCCEXpreviousCommandIndex+1);
                DCCEXpreviousCommandIndex++;
            } else {
                DCCEXsendCommandValue = DCCEXpreviousCommandList.get(0);
                DCCEXpreviousCommandIndex = 0;
            }
            etDCCEXsendCommandValue.setText(DCCEXsendCommandValue);

            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class write_tracks_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            Integer typeIndex;
            String type;
            Integer id;
            char trackLetter;

            for (int i=0; i<mainapp.DCCEX_MAX_TRACKS; i++) {
                if (mainapp.DCCEXtrackAvailable[i]) {
                    trackLetter = (char) ('A' + i);
                    typeIndex = dccExTrackTypeSpinner[i].getSelectedItemPosition();
                    type = TRACK_TYPES[typeIndex];
                    mainapp.DCCEXtrackType[i] = typeIndex;

                    if (!TRACK_TYPES_NEED_ID[typeIndex]) {
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, 0);
                    } else {
                        try {
                            id = Integer.parseInt(dccExTrackTypeIdEditText[i].getText().toString());
                            mainapp.DCCEXtrackId[i] = id.toString();
                            if (mainapp.DCCEXtrackType[i] != TRACK_TYPE_OFF_INDEX) {
                                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, id);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");
            mainapp.hideSoftKeyboard(v);
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void resetTextField(int which) {
        switch (which) {
            case WHICH_ADDRESS:
                DCCEXaddress = "";
                etDCCEXwriteAddressValue.setText("");
                break;
            case WHICH_CV:
                DCCEXcv = "";
                etDCCEXcv.setText("");
                break;
            case WHICH_CV_VALUE:
                DCCEXcvValue = "";
                etDCCEXcvValue.setText("");
                break;
            case WHICH_COMMAND:
                DCCEXsendCommandValue = "";
                etDCCEXsendCommandValue.setText("");
        }
    }

    private void readTextField(int which) {
        switch (which) {
            case WHICH_ADDRESS:
                DCCEXaddress = etDCCEXwriteAddressValue.getText().toString();
                break;
            case WHICH_CV:
                DCCEXcv = etDCCEXcv.getText().toString();
                break;
            case WHICH_CV_VALUE:
                DCCEXcvValue = etDCCEXcvValue.getText().toString();
                break;
            case WHICH_COMMAND:
                DCCEXsendCommandValue = etDCCEXsendCommandValue.getText().toString();
        }
    }

    private void showHideButtons() {
        if (dccExActionTypeIndex!=TRACK_MANAGER) {
            dexcProgrammingCommonCvsLayout.setVisibility(View.VISIBLE);
            dccExCommonCvsSpinner.setVisibility(View.VISIBLE);

            dexcProgrammingAddressLayout.setVisibility(View.VISIBLE);
            dexcProgrammingCvLayout.setVisibility(View.VISIBLE);
            dexcDCCEXtrackLinearLayout.setVisibility(View.GONE);

            sendCommandButton.setEnabled(false);
            writeAddressButton.setEnabled(DCCEXaddress.length() != 0);
            readCvButton.setEnabled(DCCEXcv.length() != 0);
            if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
                writeCvButton.setEnabled(((DCCEXcv.length() != 0) && (DCCEXcvValue.length() != 0)));
            } else {
                writeCvButton.setEnabled(((DCCEXcv.length() != 0) && (DCCEXcvValue.length() != 0) && (DCCEXaddress.length() != 0)));
            }
        } else {
            dexcProgrammingCommonCvsLayout.setVisibility(View.GONE);
            dccExCommonCvsSpinner.setVisibility(View.GONE);

            dexcProgrammingAddressLayout.setVisibility(View.GONE);
            dexcProgrammingCvLayout.setVisibility(View.GONE);
            dexcDCCEXtrackLinearLayout.setVisibility(View.VISIBLE);

            for (int i=0; i<mainapp.DCCEX_MAX_TRACKS; i++) {
                dccExTrackTypeIdEditText[i].setVisibility(TRACK_TYPES_NEED_ID[dccExTrackTypeIndex[i]] ? View.VISIBLE : View.GONE);
            }
        }
        sendCommandButton.setEnabled( (DCCEXsendCommandValue.length()!= 0) && (DCCEXsendCommandValue.charAt(0)!='<') );
        previousCommandButton.setEnabled((DCCEXpreviousCommandIndex >= 0));
        nextCommandButton.setEnabled((DCCEXpreviousCommandIndex >= 0));
    }

    public void refreshDCCEXview() {

        etDCCEXwriteAddressValue.setText(DCCEXaddress);
        DCCEXwriteInfoLabel.setText(DCCEXinfoStr);
        etDCCEXcv.setText(DCCEXcv);
        etDCCEXcvValue.setText(DCCEXcvValue);
//        etDCCEXsendCommandValue.setText(DCCEXsendCommandValue);

        if (dccExActionTypeIndex==PROGRAMMING_TRACK) {
            readAddressButton.setVisibility(View.VISIBLE);
            writeAddressButton.setVisibility(View.VISIBLE);
            readCvButton.setVisibility(View.VISIBLE);
        } else {
            readAddressButton.setVisibility(View.GONE);
            writeAddressButton.setVisibility(View.GONE);
            readCvButton.setVisibility(View.GONE);
        }

        DCCEXresponsesLabel.setText(Html.fromHtml(DCCEXresponsesStr));

        showHideButtons();

        if (menu != null) {
            mainapp.displayEStop(menu);
        }
    }

    public void refreshDCCEXtracksView() {

        for (int i=0; i<mainapp.DCCEX_MAX_TRACKS; i++) {
            dccExTrackTypeSpinner[i].setSelection(mainapp.DCCEXtrackType[i]);
            dccExTrackTypeIdEditText[i].setText(mainapp.DCCEXtrackId[i]);
            dccExTrackTypeLayout[i].setVisibility(mainapp.DCCEXtrackAvailable[i] ? View.VISIBLE : View.GONE);
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
            DCCEXresponsesListHtml.add("<small><small>" + currentTime + " </small></small> ◄ : <b>" + Html.escapeHtml(msg) + "</b><br />");
        } else {
            DCCEXresponsesListHtml.add("<small><small>" + currentTime + " </small></small> ► : &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp <i>" + Html.escapeHtml(msg) + "</i><br />");
        }
        if (DCCEXresponsesListHtml.size()>60) {
            DCCEXresponsesListHtml.remove(0);
        }

        DCCEXresponsesStr ="<p>";
        for (int i=0; i<DCCEXresponsesListHtml.size(); i++) {
            DCCEXresponsesStr = DCCEXresponsesListHtml.get(i) + DCCEXresponsesStr;
        }
        DCCEXresponsesStr = DCCEXresponsesStr + "</p>";
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
        mainapp.dcc_ex_msg_handler = new dcc_ex_handler();

        Button button;

        readAddressButton = findViewById(R.id.dexc_DCCEXreadAddressButton);
        read_address_button_listener read_address_click_listener = new read_address_button_listener();
        readAddressButton.setOnClickListener(read_address_click_listener);

        writeAddressButton = findViewById(R.id.dexc_DCCEXwriteAddressButton);
        write_address_button_listener write_address_click_listener = new write_address_button_listener();
        writeAddressButton.setOnClickListener(write_address_click_listener);

        etDCCEXwriteAddressValue = findViewById(R.id.dexc_DCCEXwriteAddressValue);
        etDCCEXwriteAddressValue.setText("");
        etDCCEXwriteAddressValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_ADDRESS); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        readCvButton = findViewById(R.id.dexc_DCCEXreadCvButton);
        read_cv_button_listener readCvClickListener = new read_cv_button_listener();
        readCvButton.setOnClickListener(readCvClickListener);

        writeCvButton = findViewById(R.id.dexc_DCCEXwriteCvButton);
        write_cv_button_listener writeCvClickListener = new write_cv_button_listener();
        writeCvButton.setOnClickListener(writeCvClickListener);

        etDCCEXcv = findViewById(R.id.dexc_DCCEXcv);
        etDCCEXcv.setText("");
        etDCCEXcv.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CV); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDCCEXcvValue = findViewById(R.id.dexc_DCCEXcvValue);
        etDCCEXcvValue.setText("");
        etDCCEXcvValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CV_VALUE); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        sendCommandButton = findViewById(R.id.dexc_DCCEXsendCommandButton);
        send_command_button_listener sendCommandClickListener = new send_command_button_listener();
        sendCommandButton.setOnClickListener(sendCommandClickListener);

        etDCCEXsendCommandValue = findViewById(R.id.dexc_DCCEXsendCommandValue);
        etDCCEXsendCommandValue.setText("");
        etDCCEXsendCommandValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_COMMAND); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        DCCEXwriteInfoLabel = findViewById(R.id.dexc_DCCEXwriteInfoLabel);
        DCCEXwriteInfoLabel.setText("");

        previousCommandButton = findViewById(R.id.dexc_DCCEXpreviousCommandButton);
        previous_command_button_listener previousCommandClickListener = new previous_command_button_listener();
        previousCommandButton.setOnClickListener(previousCommandClickListener);

        nextCommandButton = findViewById(R.id.dexc_DCCEXnextCommandButton);
        next_command_button_listener nextCommandClickListener = new next_command_button_listener();
        nextCommandButton.setOnClickListener(nextCommandClickListener);

        DCCEXresponsesLabel = findViewById(R.id.dexc_DCCEXresponsesLabel);
        DCCEXresponsesLabel.setText("");

        Button closeButton = findViewById(R.id.dcc_ex_button_close);
        close_button_listener closeClickListener = new close_button_listener();
        closeButton.setOnClickListener(closeClickListener);

        dccCvsEntryValuesArray = this.getResources().getStringArray(R.array.dccCvsEntryValues);
        final List<String> dccCvsValuesList = new ArrayList<>(Arrays.asList(dccCvsEntryValuesArray));
        dccCvsEntriesArray = this.getResources().getStringArray(R.array.dccCvsEntries); // display version
        final List<String> dccCvsEntriesList = new ArrayList<>(Arrays.asList(dccCvsEntriesArray));
        dccCvsIndex=0;
        dccExCommonCvsSpinner = findViewById(R.id.dexc_dcc_cv_list);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccCvsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExCommonCvsSpinner.setAdapter(spinner_adapter);
        dccExCommonCvsSpinner.setOnItemSelectedListener(new spinner_listener());
        dccExCommonCvsSpinner.setSelection(dccCvsIndex);

        float vn = 4;
        try {
            vn = Float.valueOf(mainapp.DCCEXversion);
        } catch (Exception e) { } // invalid version

        if (vn <= 04.002007) {  /// need to remove the track manager option
            dccExActionTypeEntryValuesArray = new String [2];
            dccExActionTypeEntriesArray = new String [2];
            for (int i=0; i<2; i++) {
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
        dexcDCCEXtrackLinearLayout = findViewById(R.id.dexc_DCCEXtrackLinearLayout);
        dexcDCCEXtracklayout[0] = findViewById(R.id.dexc_DCCEXtrack0layout);
        dexcDCCEXtracklayout[1] = findViewById(R.id.dexc_DCCEXtrack1layout);;
        dexcDCCEXtracklayout[2] = findViewById(R.id.dexc_DCCEXtrack2layout);;
        dexcDCCEXtracklayout[3] = findViewById(R.id.dexc_DCCEXtrack3layout);;
        dexcDCCEXtracklayout[4] = findViewById(R.id.dexc_DCCEXtrack4layout);;
        dexcDCCEXtracklayout[5] = findViewById(R.id.dexc_DCCEXtrack5layout);;
        dexcDCCEXtracklayout[6] = findViewById(R.id.dexc_DCCEXtrack6layout);;
        dexcDCCEXtracklayout[7] = findViewById(R.id.dexc_DCCEXtrack7layout);;

        dccExTrackTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntryValues);
        final List<String> dccTrackTypeValuesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntryValuesArray));
        dccExTrackTypeEntriesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntries); // display version
        final List<String> dccTrackTypeEntriesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntriesArray));

        for (int i=0; i<mainapp.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccExTrackTypeLayout[0] = findViewById(R.id.dexc_DCCEXtrack0layout);
                    dccExTrackTypeSpinner[0] = findViewById(R.id.dexc_track_type_0_list);
                    dccExTrackTypeIdEditText[0] = findViewById(R.id.dexc_track_0_value);
                    break;
                case 1:
                    dccExTrackTypeLayout[1] = findViewById(R.id.dexc_DCCEXtrack1layout);
                    dccExTrackTypeSpinner[1] = findViewById(R.id.dexc_track_type_1_list);
                    dccExTrackTypeIdEditText[1] = findViewById(R.id.dexc_track_1_value);
                    break;
                case 2:
                    dccExTrackTypeLayout[2] = findViewById(R.id.dexc_DCCEXtrack2layout);
                    dccExTrackTypeSpinner[2] = findViewById(R.id.dexc_track_type_2_list);
                    dccExTrackTypeIdEditText[2] = findViewById(R.id.dexc_track_2_value);
                    break;
                case 3:
                    dccExTrackTypeLayout[3] = findViewById(R.id.dexc_DCCEXtrack3layout);
                    dccExTrackTypeSpinner[3] = findViewById(R.id.dexc_track_type_3_list);
                    dccExTrackTypeIdEditText[3] = findViewById(R.id.dexc_track_3_value);
                    break;
                case 4:
                    dccExTrackTypeLayout[4] = findViewById(R.id.dexc_DCCEXtrack4layout);
                    dccExTrackTypeSpinner[4] = findViewById(R.id.dexc_track_type_4_list);
                    dccExTrackTypeIdEditText[4] = findViewById(R.id.dexc_track_4_value);
                    break;
                case 5:
                    dccExTrackTypeLayout[5] = findViewById(R.id.dexc_DCCEXtrack5layout);
                    dccExTrackTypeSpinner[5] = findViewById(R.id.dexc_track_type_5_list);
                    dccExTrackTypeIdEditText[5] = findViewById(R.id.dexc_track_5_value);
                    break;
                case 6:
                    dccExTrackTypeLayout[6] = findViewById(R.id.dexc_DCCEXtrack6layout);
                    dccExTrackTypeSpinner[6] = findViewById(R.id.dexc_track_type_6_list);
                    dccExTrackTypeIdEditText[6] = findViewById(R.id.dexc_track_6_value);
                    break;
                case 7:
                    dccExTrackTypeLayout[7] = findViewById(R.id.dexc_DCCEXtrack7layout);
                    dccExTrackTypeSpinner[7] = findViewById(R.id.dexc_track_type_7_list);
                    dccExTrackTypeIdEditText[7] = findViewById(R.id.dexc_track_7_value);
                    break;
            }
            ArrayAdapter<?> track_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExTrackTypeEntries, android.R.layout.simple_spinner_item);
            track_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dccExTrackTypeSpinner[i].setAdapter(track_type_spinner_adapter);
            dccExTrackTypeSpinner[i].setOnItemSelectedListener(new track_type_spinner_listener(dccExTrackTypeSpinner[i], i));
            dccExTrackTypeSpinner[i].setSelection(dccExTrackTypeIndex[i]);

            writeTracksButton = findViewById(R.id.dexc_DCCEXwriteTracksButton);
            write_tracks_button_listener writeTracksClickListener = new write_tracks_button_listener();
            writeTracksButton.setOnClickListener(writeTracksClickListener);
        }
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

        toolbar = findViewById(R.id.toolbar);
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
        mainapp.DCCEXscreenIsOpen = true;

        refreshDCCEXview();
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        mainapp.hideSoftKeyboard(this.getCurrentFocus());
        mainapp.DCCEXscreenIsOpen = false;
        if (mainapp.dcc_ex_msg_handler !=null) {
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
            mainapp.DCCEXscreenIsOpen = false;
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
        switch (item.getItemId()) {
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                mainapp.buttonVibration();
                return true;
            case R.id.flashlight_button:
                mainapp.toggleFlashlight(this, menu);
                mainapp.buttonVibration();
                return true;
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    mainapp.powerControlNotAllowedDialog(menu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                mainapp.buttonVibration();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            dccCvsIndex = dccExCommonCvsSpinner.getSelectedItemPosition();
            DCCEXcv = dccCvsEntryValuesArray[dccCvsIndex];
            resetTextField(WHICH_CV_VALUE);
            DCCEXinfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            refreshDCCEXview();
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
            DCCEXinfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            refreshDCCEXview();
            refreshDCCEXtracksView();
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
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            refreshDCCEXview();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}
