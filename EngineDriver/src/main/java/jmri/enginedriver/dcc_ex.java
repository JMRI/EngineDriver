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
import android.text.Html;
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

    private int dccExProgrammingTypeIndex = 0;
    String[] dccExProgrammingTypeEntryValuesArray;
    String[] dccExProgrammingTypeEntriesArray; // display version

    private static final int PROGRAMMING_TRACK = 0;
    private static final int PROGRAMMING_ON_MAIN = 1;

    Button readAddressButton;
    Button writeAddressButton;
    Button readCvButton;

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
                    refresh_dcc_ex_view();
                    break;
                case message_type.RECEIVED_CV:
                    String cvResponseStr = msg.obj.toString();
                    if (cvResponseStr.length() > 0) {
                        String[] cvArgs = cvResponseStr.split("(\\|)");
                        if ((cvArgs[0].equals(DCCEXcv)) && (!cvArgs[1].equals("-1"))) { // response matches what we got back
                            DCCEXcvValue = cvArgs[1];
                            DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXSucceeded);
                        } else {
                            DCCEXcvValue = "";
                            DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXFailed);
                        }
                        refresh_dcc_ex_view();
                    }
                    break;
                case message_type.DCCEX_RESPONSE:  // informational response
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SS");
                    String currentTime = sdf.format(new Date());

                    DCCEXresponsesListHtml.add("<small><small>" + currentTime + " : </small></small>" + Html.escapeHtml(msg.obj.toString())  + "<br />");
                    if (DCCEXresponsesListHtml.size()>20) {
                        DCCEXresponsesListHtml.remove(0);
                    }

                    DCCEXresponsesStr ="<p>";
                    for (int i=0; i<DCCEXresponsesListHtml.size(); i++) {
                        DCCEXresponsesStr = DCCEXresponsesListHtml.get(i) + DCCEXresponsesStr;
                    }
                    DCCEXresponsesStr = DCCEXresponsesStr + "</p>";
                    refresh_dcc_ex_view();

                    break;
                case message_type.WRITE_DECODER_SUCCESS:
                    DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXSucceeded);
                    refresh_dcc_ex_view();
                    break;
                case message_type.WRITE_DECODER_FAIL:
                    DCCEXinfoStr = getApplicationContext().getResources().getString(R.string.DCCEXFailed);
                    refresh_dcc_ex_view();
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refresh_dcc_ex_view();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
            }
        }
    }

    public class read_address_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            DCCEXaddress = "";
            mainapp.buttonVibration();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_DECODER_ADDRESS, "*", -1);
            refresh_dcc_ex_view();
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
                    DCCEXaddress = "";
                }
            } catch (Exception e) {
                DCCEXaddress = "";
            }
            refresh_dcc_ex_view();
        }
    }

    public class read_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            DCCEXcvValue = "";
            String cvStr = etDCCEXcv.getText().toString();
            try {
                int cv = Integer.decode(cvStr);
                if (cv > 0) {
                    DCCEXcv = Integer.toString(cv);
                    mainapp.buttonVibration();
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CV, "", cv);
                    refresh_dcc_ex_view();
                }
            } catch (Exception e) {
                DCCEXcv = "";
            }
            refresh_dcc_ex_view();
        }
    }

    public class write_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cvStr = etDCCEXcv.getText().toString();
            String cvValueStr = etDCCEXcvValue.getText().toString();
            String addrStr = etDCCEXwriteAddressValue.getText().toString();
            if (dccExProgrammingTypeIndex == PROGRAMMING_TRACK) {
                try {
                    Integer cv = Integer.decode(cvStr);
                    int cvValue = Integer.decode(cvValueStr);
                    if ((cv > 0) && (cvValue > 0)) {
                        DCCEXcv = cv.toString();
                        DCCEXcvValue = Integer.toString(cvValue);
                        mainapp.buttonVibration();
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_CV, cvValueStr, cv);
                    } else {
                        DCCEXaddress = "";
                    }
                } catch (Exception e) {
                    DCCEXaddress = "";
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
                        DCCEXaddress = "";
                    }
                } catch (Exception e) {
                    DCCEXaddress = "";
                }
            }
            refresh_dcc_ex_view();
        }
    }

    public class send_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cmdStr = etDCCEXsendCommandValue.getText().toString();
            if ((cmdStr.length()>0) && (cmdStr.charAt(0)!='<')) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_SEND_COMMAND, "<"+cmdStr+">");
                DCCEXaddress = "";
            }
            refresh_dcc_ex_view();
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    public void refresh_dcc_ex_view() {

        etDCCEXwriteAddressValue.setText(DCCEXaddress);
        DCCEXwriteInfoLabel.setText(DCCEXinfoStr);
        etDCCEXcv.setText(DCCEXcv);
        etDCCEXcvValue.setText(DCCEXcvValue);
        etDCCEXcvValue.setText(DCCEXcvValue);
        etDCCEXsendCommandValue.setText(DCCEXsendCommandValue);

        if (dccExProgrammingTypeIndex==PROGRAMMING_TRACK) {
            readAddressButton.setVisibility(View.VISIBLE);
            writeAddressButton.setVisibility(View.VISIBLE);
            readCvButton.setVisibility(View.VISIBLE);
        } else {
            readAddressButton.setVisibility(View.GONE);
            writeAddressButton.setVisibility(View.GONE);
            readCvButton.setVisibility(View.GONE);
        }

        DCCEXresponsesLabel.setText(Html.fromHtml(DCCEXresponsesStr));

        mainapp.hideSoftKeyboard(this.getCurrentFocus());

        if (menu != null) {
            mainapp.displayEStop(menu);
        }
    }

    // *************************************************************************************** //

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

        readCvButton = findViewById(R.id.dexc_DCCEXreadCvButton);
        read_cv_button_listener readCvClickListener = new read_cv_button_listener();
        readCvButton.setOnClickListener(readCvClickListener);

        button = findViewById(R.id.dexc_DCCEXwriteCvButton);
        write_cv_button_listener writeCvClickListener = new write_cv_button_listener();
        button.setOnClickListener(writeCvClickListener);

        etDCCEXcv = findViewById(R.id.dexc_DCCEXcv);
        etDCCEXcv.setText("");

        etDCCEXcvValue = findViewById(R.id.dexc_DCCEXcvValue);
        etDCCEXcvValue.setText("");

        button = findViewById(R.id.dexc_DCCEXsendCommandButton);
        send_command_button_listener sendCommandClickListener = new send_command_button_listener();
        button.setOnClickListener(sendCommandClickListener);

        etDCCEXsendCommandValue = findViewById(R.id.dexc_DCCEXsendCommandValue);
        etDCCEXsendCommandValue.setText("");

        DCCEXwriteInfoLabel = findViewById(R.id.dexc_DCCEXwriteInfoLabel);
        DCCEXwriteInfoLabel.setText("");

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

        dccExProgrammingTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExProgramingTypeEntryValues);
        final List<String> dccProgrammingTypeValuesList = new ArrayList<>(Arrays.asList(dccExProgrammingTypeEntryValuesArray));
        dccExProgrammingTypeEntriesArray = this.getResources().getStringArray(R.array.dccExProgrammingTypeEntries); // display version
        final List<String> dccProgrammingTypeEntriesList = new ArrayList<>(Arrays.asList(dccExProgrammingTypeEntriesArray));
        dccExProgrammingTypeIndex = PROGRAMMING_TRACK;
        Spinner dcc_programming_type_spinner = findViewById(R.id.dexc_programming_type_list);
        ArrayAdapter<?> programming_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExProgrammingTypeEntries, android.R.layout.simple_spinner_item);
        programming_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dcc_programming_type_spinner.setAdapter(programming_type_spinner_adapter);
        dcc_programming_type_spinner.setOnItemSelectedListener(new programming_type_spinner_listener());
        dcc_programming_type_spinner.setSelection(dccExProgrammingTypeIndex);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_dcc_ex),
                    "");
        }

    } // end onCreate

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
        }
        //update power state
        mainapp.DCCEXscreenIsOpen = true;

        refresh_dcc_ex_view();
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
        inflater.inflate(R.menu.power_menu, myMenu);
        menu = myMenu;
        mainapp.displayEStop(myMenu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);

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
            DCCEXcvValue = "";
            DCCEXinfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            refresh_dcc_ex_view();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class programming_type_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            Spinner spinner = findViewById(R.id.dexc_programming_type_list);
            dccExProgrammingTypeIndex = spinner.getSelectedItemPosition();
            DCCEXcv = "";
            DCCEXcvValue = "";
            DCCEXinfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            refresh_dcc_ex_view();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}
