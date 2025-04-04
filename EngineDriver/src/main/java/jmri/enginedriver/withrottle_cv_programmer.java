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
import android.content.SharedPreferences;
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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import jmri.enginedriver.type.message_type;
import jmri.enginedriver.util.LocaleHelper;

public class withrottle_cv_programmer extends AppCompatActivity {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu menu;
    protected SharedPreferences prefs;

//    private String witCv = "";
//    private String witCvValue = "";

    private EditText etWitCv;
    private EditText etWitCvValue;

//    private String witAddress = "";
    private EditText etWitAddressValue;


    private String witInfoStr = "";

    private String witSendCommandValue = "";
    private EditText etWitSendCommandValue;

    private LinearLayout witWriteInfoLayout;
    private TextView witWriteInfoLabel;

//    private TextView witResponsesLabel;
    private TextView witSendsLabel;
    private String witResponsesStr = "";
    private String witSendsStr = "";
//    private ScrollView witResponsesScrollView;
    private ScrollView witSendsScrollView;

    ArrayList<String> witResponsesListHtml = new ArrayList<>();
    ArrayList<String> witSendsListHtml = new ArrayList<>();

    private int dccCvsIndex = 0;
    String[] dccCvsEntryValuesArray;
    String[] dccCvsEntriesArray; // display version

    Button writeCvButton;
    Button clearCommandsButton;

    Spinner addressSpinner;

    private LinearLayout witProgrammingCommonCvsLayout;
    private LinearLayout witProgrammingAddressLayout;
    private LinearLayout witProgrammingCvLayout;
    Spinner witCommonCvsSpinner;

    static final int WHICH_ADDRESS = 0;
    static final int WHICH_CV = 1;
    static final int WHICH_CV_VALUE = 2;

    String cv29SpeedSteps;
    String cv29AnalogueMode;
    String cv29Direction;
    String cv29AddressSize;
    String cv29SpeedTable;

    float vn = 4; // DCC-EC Version number

    /** @noinspection FieldCanBeLocal*/
    private LinearLayout screenNameLine;
    /** @noinspection FieldCanBeLocal*/
    private Toolbar toolbar;
    /** @noinspection FieldCanBeLocal*/
    private LinearLayout statusLine;


    private String defaultAddressLength;
    private int maxAddr = 10239;
    private int maxCv = 1024;
    private int maxCvValue = 255;

    //**************************************


    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class withrottle_cv_programmer_handler extends Handler {

        public withrottle_cv_programmer_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.WRITE_DIRECT_DCC_COMMAND_ECHO:  // informational response
                    displayCommands(msg.obj.toString(), false);
                    refreshWitCommandsView();
                    break;

                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refreshWitView();
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
                    }
                    break;
            }
        }
    }

    public class write_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            witInfoStr = "";
            String cvStr = etWitCv.getText().toString();
            String cvValueStr = etWitCvValue.getText().toString();
            String addrStr = etWitAddressValue.getText().toString();

            try {
                Integer cv = Integer.decode(cvStr);
                int cvValue = Integer.decode(cvValueStr);
                int addr = Integer.decode(addrStr);
                if ((addr > 2) && (addr <= 10239) && (cv > 0)) {
                    mainapp.witAddress = Integer.toString(addr);
                    mainapp.witCv = cv.toString();
                    mainapp.witCvValue = Integer.toString(cvValue);
                    mainapp.buttonVibration();
//                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_POM_CV, witCv + " " + witCvValue, addr);

                    Integer[] segments = new Integer[6];
                    int noSegments = 0;
                    String directCmd = "";
                    String bits = "";

                    int addressSize = addressSpinner.getSelectedItemPosition();

                    if (addressSize==0) {  // short
                        bits = "0" + num2binStr(addr,7);
                        segments[noSegments] = str2Bin(bits);
                        noSegments++;
                        directCmd = bits + " ";
                    } else {
                        String addrBits = num2binStr(addr,14);
                        bits = "11" + addrBits.substring(0,6);
                        segments[noSegments] = str2Bin(bits);
                        noSegments++;
                        directCmd = bits  + " ";

                        bits = addrBits.substring(6);
                        segments[noSegments] = str2Bin(bits);
                        noSegments++;
                        directCmd = directCmd + bits  + " ";
                    }
                    String cvBits = num2binStr(cv - 1,10);

                    bits = "111011" + cvBits.substring(0,2);
                    segments[noSegments] = str2Bin(bits);
                    noSegments++;
                    directCmd = directCmd + bits + " ";

                    bits = cvBits.substring(2);
                    segments[noSegments] = str2Bin(bits);
                    noSegments++;
                    directCmd = directCmd + bits + " ";

                    bits = num2binStr(cvValue,8);
                    segments[noSegments] = str2Bin(bits);
                    noSegments++;
                    directCmd = directCmd + bits + " ";

                    int xOr = segments[0];
                    for (int i=1; i<noSegments; i++) {
                        xOr = (xOr ^ segments[i]);
                    }

                    bits = num2binStr(xOr,8);
                    segments[noSegments] = str2Bin(bits);
                    noSegments++;
                    directCmd = directCmd + bits;

                    String msg = "";
                    if (noSegments==5) {
                        msg= String.format("%02x %02x %02x %02x %02x", segments[0], segments[1], segments[2], segments[3], segments[4]);
                    } else {
                        msg= String.format("%02x %02x %02x %02x %02x %02x", segments[0], segments[1], segments[2], segments[3], segments[4], segments[5]);
                    }
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_DIRECT_DCC_COMMAND, msg, addr);
                } else {
                    resetTextField(WHICH_ADDRESS);
                }
            } catch (Exception e) {
                resetTextField(WHICH_ADDRESS);
            }

            refreshWitView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class clear_commands_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            witResponsesListHtml.clear();
            witSendsListHtml.clear();
            witResponsesStr = "";
            witSendsStr = "";
            refreshWitView();
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
                mainapp.witAddress = "";
                etWitAddressValue.setText("");
                break;
            case WHICH_CV:
                mainapp.witCv = "";
                etWitCv.setText("");
                break;
            case WHICH_CV_VALUE:
                mainapp.witCvValue = "";
                etWitCvValue.setText("");
                break;
        }
    }

    private void readTextField(int which) {
        String txt = "";
        int txtLen = 0;
        int addr = -1;

        switch (which) {
            case WHICH_ADDRESS:
                mainapp.witAddress = etWitAddressValue.getText().toString();

                txt = mainapp.witAddress.trim();
                txtLen = txt.length();
                if (txtLen > 0) {
                    try {
                        addr = Integer.parseInt(txt);
                    } catch (NumberFormatException e) {
                        etWitAddressValue.setText(""); //clear the bad entry
                    }

                    if (addr>maxAddr) {
                        addr = -1;
//                        Toast.makeText(getApplicationContext(),
//                                getApplicationContext().getResources().getString(R.string.toastAddressExceedsMax, txt, Integer.toString(maxAddr))
//                                , Toast.LENGTH_LONG).show();
                        threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastAddressExceedsMax,
                                                    txt, Integer.toString(maxAddr)), Toast.LENGTH_LONG);
                        etWitAddressValue.setText(""); //clear the bad entry
                    }
                }


                defaultAddressLength = prefs.getString("default_address_length", this
                        .getResources().getString(R.string.prefDefaultAddressLengthDefaultValue));
                // set address length
                if (defaultAddressLength.equals("Long") ||
                        (defaultAddressLength.equals("Auto") && (addr > 127))) {
                    addressSpinner.setSelection(1);
                } else {
                    addressSpinner.setSelection(0);
                }

                break;
            case WHICH_CV:
                mainapp.witCv = etWitCv.getText().toString();

                txt = mainapp.witCv.trim();
                txtLen = txt.length();
                if (txtLen > 0) {
                    try {
                        addr = Integer.parseInt(txt);
                    } catch (NumberFormatException e) {
                        etWitCv.setText(""); //clear the bad entry
                    }

                    if (addr>maxCv) {
                        addr = -1;
//                        Toast.makeText(getApplicationContext(),
//                                getApplicationContext().getResources().getString(R.string.toastAddressExceedsMax, txt, Integer.toString(maxCv))
//                                , Toast.LENGTH_LONG).show();
                        threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastAddressExceedsMax, txt, Integer.toString(maxCv))
                                , Toast.LENGTH_LONG);
                        etWitCv.setText(""); //clear the bad entry
                    }
                }

                break;
            case WHICH_CV_VALUE:
                mainapp.witCvValue = etWitCvValue.getText().toString();

                txt = mainapp.witCvValue.trim();
                txtLen = txt.length();
                if (txtLen > 0) {
                    try {
                        addr = Integer.parseInt(txt);
                    } catch (NumberFormatException e) {
                        etWitCvValue.setText(""); //clear the bad entry
                    }

                    if (addr>maxCvValue) {
                        addr = -1;
//                        Toast.makeText(getApplicationContext(),
//                                getApplicationContext().getResources().getString(R.string.toastAddressExceedsMax, txt, Integer.toString(maxCvValue))
//                                , Toast.LENGTH_LONG).show();
                        threaded_application.safeToast(String.format(getApplicationContext().getResources().getString(R.string.toastAddressExceedsMax), txt, Integer.toString(maxCvValue))
                                , Toast.LENGTH_LONG);
                        etWitCvValue.setText(""); //clear the bad entry
                    }
                }

                break;
        }
    }

    private void showHideButtons() {
        witProgrammingCommonCvsLayout.setVisibility(View.VISIBLE);
        witCommonCvsSpinner.setVisibility(View.VISIBLE);
        int addressSize = addressSpinner.getSelectedItemPosition();

        witProgrammingAddressLayout.setVisibility(View.VISIBLE);
        witProgrammingCvLayout.setVisibility(View.VISIBLE);
        witWriteInfoLayout.setVisibility(View.VISIBLE);

        boolean rslt = true;
        if (mainapp.witAddress.length()>0) {
            int addressValue = Integer.parseInt(mainapp.witAddress);
            if (addressValue < 2) rslt = false;

            if (rslt) {
                if ( (addressValue>127) && (addressSize==0) ) rslt = false;
            }
        }
        if (rslt) {
            rslt = ((mainapp.witCv.length() != 0) && (mainapp.witCvValue.length() != 0) && (mainapp.witAddress.length() != 0));
        }
        if (rslt) {
            if ((mainapp.witCv.equals("1")) || (mainapp.witCv.equals("17")) || (mainapp.witCv.equals("18"))) rslt = false;
        }
        if (rslt) {
            if ((mainapp.witCv.equals("29")) && (!checkCv29addressUnchanged())) rslt = false;
        }
        writeCvButton.setEnabled(rslt);
    }

    public void refreshWitView() {

        witWriteInfoLabel.setText(witInfoStr);
        etWitCv.setText(mainapp.witCv);
        etWitCvValue.setText(mainapp.witCvValue);

        refreshWitCommandsView();

        showHideButtons();

        if (menu != null) {
            mainapp.displayEStop(menu);
        }
    }

    public void refreshWitCommandsView() {
//        witResponsesLabel.setText(Html.fromHtml(witResponsesStr));
        witSendsLabel.setText(Html.fromHtml(witSendsStr));
    }

    void displayCommands(String msg, boolean inbound) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String currentTime = sdf.format(new Date());

        if (inbound) {
            witResponsesListHtml.add("<small><small>" + currentTime + " </small></small> ◄ : <b>" + Html.escapeHtml(msg) + "</b><br />");
        } else {
            witSendsListHtml.add("<small><small>" + currentTime + " </small></small> ► : <i>" + Html.escapeHtml(msg) + "</i><br />");
        }
        if (witResponsesListHtml.size() > 40) {
            witResponsesListHtml.remove(0);
        }
        if (witSendsListHtml.size() > 30) {
            witSendsListHtml.remove(0);
        }

        witResponsesStr = "<p>";
        for (int i = 0; i < witResponsesListHtml.size(); i++) {
            witResponsesStr = witResponsesListHtml.get(i) + witResponsesStr;
        }
        witResponsesStr = witResponsesStr + "</p>";

        witSendsStr = "<p>";
        for (int i=0; i < witSendsListHtml.size(); i++) {
            witSendsStr = witSendsListHtml.get(i) + witSendsStr;
        }
        witSendsStr = witSendsStr + "</p>";
    }

    // ************************************************************************************************************* //

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        mainapp.applyTheme(this);

        setContentView(R.layout.withrottle_cv_programmer);

        //put pointer to this activity's handler in main app's shared variable (If needed)
        mainapp.withrottle_cv_programmer_msg_handler = new withrottle_cv_programmer_handler(Looper.getMainLooper());

        etWitAddressValue = findViewById(R.id.wit_WitWriteAddressValue);
        etWitAddressValue.setText("");
        etWitAddressValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_ADDRESS); checkCv29addressUnchanged(); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        // Set the options for the address length.
        addressSpinner = findViewById(R.id.wit_addressLength);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this,
                R.array.address_size, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addressSpinner.setAdapter(spinner_adapter);
        addressSpinner.setOnItemSelectedListener(new addressLengthSpinnerListener());

        writeCvButton = findViewById(R.id.wit_WitWriteCvButton);
        write_cv_button_listener writeCvClickListener = new write_cv_button_listener();
        writeCvButton.setOnClickListener(writeCvClickListener);

        etWitCv = findViewById(R.id.wit_WitCv);
        etWitCv.setText("");
        etWitCv.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CV); checkCv29(mainapp.witCv, mainapp.witCvValue); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etWitCvValue = findViewById(R.id.wit_WitCvValue);
        etWitCvValue.setText("");
        etWitCvValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CV_VALUE); checkCv29(mainapp.witCv, mainapp.witCvValue); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        witWriteInfoLayout = findViewById(R.id.witWriteInfoLayout);
        witWriteInfoLabel = findViewById(R.id.witWriteInfoLabel);
        witWriteInfoLabel.setText("");

//        witResponsesLabel = findViewById(R.id.wit_WitResponsesLabel);
//        witResponsesLabel.setText("");
        witSendsLabel = findViewById(R.id.wit_WitSendsLabel);
        witSendsLabel.setText("");

        Button closeButton = findViewById(R.id.withrottle_cv_programmer_button_close);
        close_button_listener closeClickListener = new close_button_listener();
        closeButton.setOnClickListener(closeClickListener);

        dccCvsEntryValuesArray = this.getResources().getStringArray(R.array.dccCvsEntryValues);
//        final List<String> dccCvsValuesList = new ArrayList<>(Arrays.asList(dccCvsEntryValuesArray));
        dccCvsEntriesArray = this.getResources().getStringArray(R.array.dccCvsEntries); // display version
//        final List<String> dccCvsEntriesList = new ArrayList<>(Arrays.asList(dccCvsEntriesArray));

        dccCvsIndex = 0;
        witCommonCvsSpinner = findViewById(R.id.wit_dccCvList);
        spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccCvsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        witCommonCvsSpinner.setAdapter(spinner_adapter);
        witCommonCvsSpinner.setOnItemSelectedListener(new commonCvsSpinnerListener());
        witCommonCvsSpinner.setSelection(dccCvsIndex);

        vn = 4;
        try {
            vn = Float.valueOf(mainapp.DccexVersion);
        } catch (Exception ignored) { } // invalid version

        witProgrammingCommonCvsLayout = findViewById(R.id.wit_programmingCommonCvsLayout);
        witProgrammingAddressLayout = findViewById(R.id.wit_programmingAddressLayout);
        witProgrammingCvLayout = findViewById(R.id.wit_programmingCvLayout);


//        witResponsesScrollView = findViewById(R.id.wit_WitResponsesScrollView);
        witSendsScrollView = findViewById(R.id.wit_WitSendsScrollView);

        clearCommandsButton = findViewById(R.id.wit_WitClearCommandsButton);
        clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_withrottle_cv_programmer),
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

        etWitAddressValue.setText(mainapp.witAddress);
        etWitCv.setText(mainapp.witCv);
        etWitCvValue.setText(mainapp.witCvValue);

        if (menu != null) {
            mainapp.displayEStop(menu);
            mainapp.displayFlashlightMenuButton(menu);
            mainapp.setFlashlightButton(menu);
            mainapp.displayPowerStateMenuButton(menu);
            mainapp.setPowerStateButton(menu);
        }
        //update power state
        mainapp.witScreenIsOpen = true;

        refreshWitView();
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        mainapp.hideSoftKeyboard(this.getCurrentFocus());
        mainapp.witScreenIsOpen = false;
        if (mainapp.withrottle_cv_programmer_msg_handler != null) {
            mainapp.withrottle_cv_programmer_msg_handler.removeCallbacksAndMessages(null);
            mainapp.withrottle_cv_programmer_msg_handler = null;
        } else {
            Log.d("Engine_Driver", "onDestroy: mainapp.withrottle_cv_programmer_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu myMenu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.withrottle_cv_programmer_menu, myMenu);
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
            mainapp.witScreenIsOpen = false;
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

    public class addressLengthSpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            showHideButtons();
            refreshWitView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class commonCvsSpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            dccCvsIndex = witCommonCvsSpinner.getSelectedItemPosition();
            if (dccCvsIndex > 0) {
                mainapp.witCv = dccCvsEntryValuesArray[dccCvsIndex];
                resetTextField(WHICH_CV_VALUE);
                etWitCvValue.requestFocus();
            }
            dccCvsIndex = 0;
            witCommonCvsSpinner.setSelection(dccCvsIndex);
            witInfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            showHideButtons();
            refreshWitView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    void checkCv29(String cv, String cvValueStr) {
        if (cv.equals("29") && cvValueStr.length()>0) {
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

                witSendsStr = "<p>" + rslt + "</p>" + witSendsStr;

                witSendsStr = "<p>CV 29 value: " + cvValueStr + " = </p>" + witSendsStr;

//                witSendsStr = "<p>"
//                        + String.format(getApplicationContext().getResources().getString(R.string.cv29SpeedToggleDirection),
//                        mainapp.toggleBit(cvValue, 1))
//                        + "</p>" + witSendsStr;

                refreshWitCommandsView();

            } catch (Exception e) {
                Log.e("EX_Toolbox", "Error processing cv29: " + e.getMessage());
            }
        }
    }

    boolean checkCv29addressUnchanged() {
        boolean rslt = true;
        if (mainapp.witCv.equals("29") && mainapp.witCvValue.length()>0) {
            int cvValue = Integer.parseInt(mainapp.witCvValue);

            int addressSize = addressSpinner.getSelectedItemPosition();

            if (mainapp.bitExtracted(cvValue, 1, 6) == 0) {  // short
                if (addressSize == 1) rslt = false; // long selected
            } else { // long
                if (addressSize == 0) rslt = false; // short selected
            }
        }
        return rslt;
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

   String num2binStr(int val, int bits) { // bits = number of bits to return - dictates the max val allowed
       String rslt = "";
       Double tempVal = (double) val;
       for (int i = bits; i >= 0; i--) {
           if (tempVal >= Math.pow(2, i)) {
               rslt = rslt + "1";
               tempVal = tempVal - Math.pow(2, i);
           } else {
               rslt = rslt + "0";
           }
       }
       while (rslt.length() > bits) {
           rslt = rslt.substring(1);  // remove the leading 0
       }
       return rslt;
   }

   int str2Bin(String val) {
       Double rslt = 0.0;
        for (int i=0; i<val.length(); i++) {
            if (val.charAt(i)=='1') {
                rslt = rslt + Math.pow(2,val.length()-1-i);
            }
        }
       return rslt.intValue();
   }
}
