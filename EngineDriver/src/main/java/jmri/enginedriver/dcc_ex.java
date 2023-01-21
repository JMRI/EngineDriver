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

    private LinearLayout dexcProgrammingAddressLayout;
    private LinearLayout dexcProgrammingCvLayout;
    private LinearLayout[] dexcDCCEXtracklayout = {null, null, null, null};

    private int[] dccExTrackTypeIndex = {1, 2, 1, 1};
    String[] dccExTrackTypeEntryValuesArray;
    String[] dccExTrackTypeEntriesArray; // display version

    private int DCCEXpreviousCommandIndex = -1;
    ArrayList<String> DCCEXpreviousCommandList = new ArrayList<>();

    static final int WHICH_ADDRESS = 0;
    static final int WHICH_CV = 1;
    static final int WHICH_CV_VALUE = 2;
    static final int WHICH_COMMAND = 3;

    static final int TRACK_TYPE_OFF = 0;
    static final int TRACK_TYPE_DCC_MAIN = 1;
    static final int TRACK_TYPE_DCC_PROG = 2;
    static final int TRACK_TYPE_DC = 3;
    static final int TRACK_TYPE_DCX = 4;

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
                case message_type.DCCEX_RESPONSE:  // informational response
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SS");
                    String currentTime = sdf.format(new Date());

                    DCCEXresponsesListHtml.add("<small><small>" + currentTime + " : </small></small>" + Html.escapeHtml(msg.obj.toString())  + "<br />");
                    if (DCCEXresponsesListHtml.size()>40) {
                        DCCEXresponsesListHtml.remove(0);
                    }

                    DCCEXresponsesStr ="<p>";
                    for (int i=0; i<DCCEXresponsesListHtml.size(); i++) {
                        DCCEXresponsesStr = DCCEXresponsesListHtml.get(i) + DCCEXresponsesStr;
                    }
                    DCCEXresponsesStr = DCCEXresponsesStr + "</p>";
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
        }
    }

    public class write_address_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String addrStr = etDCCEXwriteAddressValue.getText().toString();
            try {
                Integer addr = Integer.decode(addrStr);
                if ((addr>2) && (addr<=9999)) {
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
        }
    }

    public class send_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cmdStr = etDCCEXsendCommandValue.getText().toString();
            if ((cmdStr.length()>0) && (cmdStr.charAt(0)!='<')) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_SEND_COMMAND, "<"+cmdStr+">");

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
            dexcProgrammingAddressLayout.setVisibility(View.VISIBLE);
            dexcProgrammingCvLayout.setVisibility(View.VISIBLE);
            for (int i=0; i<4; i++) {
                dexcDCCEXtracklayout[i].setVisibility(View.GONE);
            }
            sendCommandButton.setEnabled(false);
            writeAddressButton.setEnabled(DCCEXaddress.length() != 0);
            readCvButton.setEnabled(DCCEXcv.length() != 0);
            if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
                writeCvButton.setEnabled(((DCCEXcv.length() != 0) && (DCCEXcvValue.length() != 0)));
            } else {
                writeCvButton.setEnabled(((DCCEXcv.length() != 0) && (DCCEXcvValue.length() != 0) && (DCCEXaddress.length() != 0)));
            }
            sendCommandButton.setEnabled(DCCEXsendCommandValue.length() != 0);
            previousCommandButton.setEnabled((DCCEXpreviousCommandIndex >= 0));
            nextCommandButton.setEnabled((DCCEXpreviousCommandIndex >= 0));
        } else {
            dexcProgrammingAddressLayout.setVisibility(View.GONE);
            dexcProgrammingCvLayout.setVisibility(View.GONE);
            for (int i=0; i<4; i++) {
                dexcDCCEXtracklayout[i].setVisibility(View.VISIBLE);
            }

        }
    }

    public void refreshDCCEXview() {

        etDCCEXwriteAddressValue.setText(DCCEXaddress);
        DCCEXwriteInfoLabel.setText(DCCEXinfoStr);
        etDCCEXcv.setText(DCCEXcv);
        etDCCEXcvValue.setText(DCCEXcvValue);
        etDCCEXsendCommandValue.setText(DCCEXsendCommandValue);

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

        mainapp.hideSoftKeyboard(this.getCurrentFocus());

        if (menu != null) {
            mainapp.displayEStop(menu);
        }
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
        Spinner dcc_cvs_spinner = findViewById(R.id.dexc_dcc_cv_list);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccCvsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dcc_cvs_spinner.setAdapter(spinner_adapter);
        dcc_cvs_spinner.setOnItemSelectedListener(new spinner_listener());
        dcc_cvs_spinner.setSelection(dccCvsIndex);

        dccExActionTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues);
        final List<String> dccActionTypeValuesList = new ArrayList<>(Arrays.asList(dccExActionTypeEntryValuesArray));
        dccExActionTypeEntriesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntries); // display version
        final List<String> dccActionTypeEntriesList = new ArrayList<>(Arrays.asList(dccExActionTypeEntriesArray));
        dccExActionTypeIndex = PROGRAMMING_TRACK;
        Spinner dcc_action_type_spinner = findViewById(R.id.dexc_action_type_list);
        ArrayAdapter<?> action_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExActionTypeEntries, android.R.layout.simple_spinner_item);
        action_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dcc_action_type_spinner.setAdapter(action_type_spinner_adapter);
        dcc_action_type_spinner.setOnItemSelectedListener(new action_type_spinner_listener());
        dcc_action_type_spinner.setSelection(dccExActionTypeIndex);

        dexcProgrammingAddressLayout = findViewById(R.id.dexc_programmingAddressLayout);;
        dexcProgrammingCvLayout = findViewById(R.id.dexc_programmingCvLayout);
        dexcDCCEXtracklayout[0] = findViewById(R.id.dexc_DCCEXtrack0layout);;
        dexcDCCEXtracklayout[1] = findViewById(R.id.dexc_DCCEXtrack1layout);;
        dexcDCCEXtracklayout[2] = findViewById(R.id.dexc_DCCEXtrack2layout);;
        dexcDCCEXtracklayout[3] = findViewById(R.id.dexc_DCCEXtrack3layout);;

        dccExTrackTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntryValues);
        final List<String> dccTrackTypeValuesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntryValuesArray));
        dccExTrackTypeEntriesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntries); // display version
        final List<String> dccTrackTypeEntriesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntriesArray));

        Spinner dcc_track_type_spinner;
        for (int i=0; i<4; i++) {
            switch (i) {
                default:
                case 0: dcc_track_type_spinner = findViewById(R.id.dexc_track_type_0_list); break;
                case 1: dcc_track_type_spinner = findViewById(R.id.dexc_track_type_1_list); break;
                case 2: dcc_track_type_spinner = findViewById(R.id.dexc_track_type_2_list); break;
                case 3: dcc_track_type_spinner = findViewById(R.id.dexc_track_type_3_list); break;
            }
            ArrayAdapter<?> track_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExTrackTypeEntries, android.R.layout.simple_spinner_item);
            track_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dcc_track_type_spinner.setAdapter(track_type_spinner_adapter);
            dcc_track_type_spinner.setOnItemSelectedListener(new track_type_spinner_listener(dcc_track_type_spinner, i));
            dcc_track_type_spinner.setSelection(dccExTrackTypeIndex[i]);

        }

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

            Spinner spinner = findViewById(R.id.dexc_dcc_cv_list);
            dccCvsIndex = spinner.getSelectedItemPosition();
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
