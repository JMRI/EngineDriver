package jmri.enginedriver.logviewer.ui;

import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import jmri.enginedriver.R;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;

//import jmri.enginedriver.logviewer.R;

/** @noinspection CallToPrintStackTrace*/
public class LogViewerActivity extends AppCompatActivity implements PermissionsHelper.PermissionsHelperGrantedCallback {
    private ArrayAdapter adaptor = null;
    private LogReaderTask logReaderTask = null;
    private threaded_application mainapp;  // hold pointer to mainapp

    private Button saveButton;
    private TextView saveInfoTV;

//    private static final String ENGINE_DRIVER_DIR = "Android\\data\\jmri.enginedriver\\files";

    private Menu AMenu;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        mainapp.applyTheme(this);
        setContentView(R.layout.log_main);

        final ListView listView = findViewById(android.R.id.list);

        ArrayList<String> logArray = new ArrayList<>();
        adaptor = new LogStringAdaptor(this, R.layout.logitem, logArray);

        listView.setAdapter(adaptor);

        listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(LogViewerActivity.this);
                String text = ((TextView) view).getText().toString();
                builder.setMessage(text);
                builder.show();
                mainapp.buttonVibration();
            }
        } );

        //Set the buttons
        Button closeButton = findViewById(R.id.logviewer_button_close);
        close_button_listener close_click_listener = new close_button_listener();
        closeButton.setOnClickListener(close_click_listener);

//        Button resetButton = findViewById(R.id.logviewer_button_reset);
//        reset_button_listener reset_click_listener = new reset_button_listener();
//        resetButton.setOnClickListener(reset_click_listener);

        saveButton = findViewById(R.id.logviewer_button_save);
        save_button_listener save_click_listener = new save_button_listener();
        saveButton.setOnClickListener(save_click_listener);
        saveInfoTV = findViewById(R.id.logviewer_info);
        showHideSaveButton();

        logReaderTask = new LogReaderTask();

        logReaderTask.execute();

        //put pointer to this activity's handler in main app's shared variable
        mainapp.logviewer_msg_handler = new logviewer_handler(Looper.getMainLooper());

        LinearLayout screenNameLine = findViewById(R.id.screen_name_line);
        Toolbar toolbar = findViewById(R.id.toolbar);
        LinearLayout statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();

            SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
            String name = prefs.getString("throttle_name_preference", "");

            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name) + " | " + name,
                    getApplicationContext().getResources().getString(R.string.app_name_log_viewer),
                    "");
        }

        Log.d("Engine_Driver", mainapp.getAboutInfo());
        Log.d("Engine_Driver", mainapp.getAboutInfo(false));

    } // end onCreate

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) {        //expedite
            this.finish();
        }

        if (AMenu != null) {
            mainapp.displayFlashlightMenuButton(AMenu);
            mainapp.setFlashlightButton(AMenu);
//            mainapp.displayPowerStateMenuButton(AMenu);
//            mainapp.setPowerStateButton(AMenu);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logviewer_menu, menu);
        mainapp.displayEStop(menu);
        AMenu = menu;
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);

        return  super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.EmerStop) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlight(this, AMenu);
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.power_layout_button) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(AMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onDestroy() {
        Log.d("Engine_Driver", "log_viewer.onDestroy() called");

        if (logReaderTask != null ) {
            logReaderTask.stopTask();
        }
        super.onDestroy();
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

//    public class reset_button_listener implements View.OnClickListener {
//        public void onClick(View v) {
//
//        }
//    }

    @SuppressLint("HandlerLeak")
    class logviewer_handler extends Handler {

        public logviewer_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (msg.what) {
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateButton(AMenu);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    public class save_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            saveLogFile();
        }
    }

    private void saveLogFile() {
        File logFile = new File(context.getExternalFilesDir(null), "logcat" + System.currentTimeMillis() + ".txt");

        try {
            Process process = Runtime.getRuntime().exec("logcat -c");
            Runtime.getRuntime().exec("logcat -f " + logFile);
//            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastSaveLogFile, logFile.toString()), Toast.LENGTH_LONG).show();
            threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastSaveLogFile, logFile.toString()), Toast.LENGTH_LONG);
            mainapp.logSaveFilename = logFile.toString();
            showHideSaveButton();
            Log.d("Engine_Driver", "Logging started to: " + logFile);
            Log.d("Engine_Driver", mainapp.getAboutInfo());
            Log.d("Engine_Driver", mainapp.getAboutInfo(false));
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    void showHideSaveButton() {
        if (mainapp.logSaveFilename.length()>0) {
            saveButton.setVisibility(View.GONE);
            saveInfoTV.setText(String.format(getApplicationContext().getResources().getString(R.string.infoSaveLogFile), mainapp.logSaveFilename) );
            saveInfoTV.setVisibility(View.VISIBLE);
        } else {
            saveButton.setVisibility(View.VISIBLE);
            saveInfoTV.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@PermissionsHelper.RequestCodes int requestCode) {
        Log.d("Engine_Driver", "LogViewerActivity: navigateToHandler:" + requestCode);
        if (!PermissionsHelper.getInstance().isPermissionGranted(LogViewerActivity.this, requestCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(LogViewerActivity.this, requestCode);
            }
        } else {
            // Go to the correct handler based on the request code.
            // Only need to consider relevant request codes initiated by this Activity
            //noinspection SwitchStatementWithTooFewBranches
            switch (requestCode) {
//                case PermissionsHelper.STORE_LOG_FILES:
//                    Log.d("Engine_Driver", "Preferences: Got permission for STORE_LOG_FILES - navigate to writeSharedPreferencesToFileImpl()");
//                    saveLogFileImpl();
//                    break;
                default:
                    // do nothing
                    Log.d("Engine_Driver", "Preferences: Unrecognised permissions request code: " + requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(LogViewerActivity.this, requestCode, permissions, grantResults)) {
            Log.d("Engine_Driver", "Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private class LogStringAdaptor extends ArrayAdapter<String> {
        private final List<String> objects;

        public LogStringAdaptor(Context context, int textViewId, List<String> objects) {
            super(context, textViewId, objects);

            this.objects = objects;
        }

        @Override
        public int getCount() {
            return ((null != objects) ? objects.size() : 0);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public String getItem(int position) {
            return ((null != objects) ? objects.get(position) : null);
        }

        @SuppressLint("InflateParams")
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (null == view) {
                LayoutInflater vi = (LayoutInflater) LogViewerActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.logitem, null);
            }

            String data = objects.get(position);

            if (null != data) {
                TextView textview = view.findViewById(R.id.txtLogString);
                String msg = data;
                int msgStart = data.indexOf("Engine_Driver: "); //post-marshmallow format
                if (msgStart > 0) {
                    msg = data.substring(msgStart + 15);
                    if (mainapp.prefShowTimeOnLogEntry) {
                        int tmStart = data.indexOf(" "); //post-marshmallow format
                        String tm = data.substring(tmStart + 1,tmStart+12);
                        msg = tm + " " + msg;
                    }
                } else {
                    msgStart = data.indexOf("): "); //pre-marshmallow format
                    if (msgStart > 0) {
                        msg = data.substring(msgStart + 3);
                    }
                }
                if (!msg.substring(0,6).equals("<span>")) {
                    msg = msg.replaceAll("&", "&amp;");
                    msg = msg.replaceAll("<", "&lt;");
                    msg = msg.replaceAll(">", "&gt;");
                    if (msg.indexOf("About: ") < 0) {
                        if (mainapp.getSelectedTheme() == R.style.app_theme_colorful) {
                            msg = "<span style=\"color: #404040\">" + msg;
                        } else {
                            msg = "<span style=\"color: #CCCCCC\">" + msg;
                        }
                    } else {
                        msg = "<br/><span>" + msg;
                    }
                    if (msg.indexOf("--&gt;") > 0) {
                        msg = msg.replace("--&gt;", "</span><br/><b>--&gt;") + "</b>";
                    } else if (msg.indexOf("&lt;--") > 0) {
                        msg = msg.replace("&lt;--", "</span><br/><b>&lt;--") + "</b>";
                    } else {
                        msg = msg + "</span>";
                    }
                }
                textview.setText(Html.fromHtml(msg));
                return view;
            }
            return null;

        }
    }

    /** @noinspection CallToPrintStackTrace*/
    private class LogReaderTask extends AsyncTask<Void, String, Void> {
        private final String[] LOGCAT_CMD = new String[]{"logcat", "Engine_Driver:D", "*:S"};
        //		private final int BUFFER_SIZE = 1024;

        private boolean isRunning = true;
        private Process logprocess = null;
        private BufferedReader reader = null;
        private String line = "";
        //		private String lastLine = "";

        @Override
        protected Void doInBackground(Void... params) {
            try {
                logprocess = Runtime.getRuntime().exec(LOGCAT_CMD);
            } catch (IOException e) {
                e.printStackTrace();

                isRunning = false;
            }

            try {
                //				reader = new BufferedReader(new InputStreamReader(
                //						logprocess.getInputStream()),BUFFER_SIZE);
                reader = new BufferedReader(new InputStreamReader(
                        logprocess.getInputStream()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();

                isRunning = false;
            }

            line = "";
            //			lastLine = new String;

            try {
                while (isRunning) {
                    line = reader.readLine();
                    publishProgress(line);
                }
            } catch (IOException e) {
                e.printStackTrace();

                isRunning = false;
            }
            finally {
                if(reader != null) {
                    try {
                        reader.close();
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //			if ((values[0] != null) && !values[0].equals(lastLine)) {
            if ((values[0] != null)) {
                adaptor.add(values[0]);
                adaptor.notifyDataSetChanged();
            }
            //			lastLine = values[0];
        }

        public void stopTask() {
            isRunning = false;
            if (logprocess != null) logprocess.destroy();
        }
    }
}