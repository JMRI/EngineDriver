package jmri.enginedriver.logviewer.ui;

import static android.view.KeyEvent.KEYCODE_BACK;
import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jmri.enginedriver.R;
import jmri.enginedriver.connection_activity;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;

//import jmri.enginedriver.logviewer.R;

/** @noinspection CallToPrintStackTrace*/
public class LogViewerActivity extends AppCompatActivity implements PermissionsHelper.PermissionsHelperGrantedCallback {
    static final String activityName = "LogViewerActivity";

    private ArrayAdapter<String> adaptor = null;
    private LogReaderTask logReaderTask = null;
    private threaded_application mainapp;  // hold pointer to mainapp

    private Button saveButton;
    private Button stopSaveButton;
    private Button shareButton;
    private TextView saveInfoTV;

//    private static final String ENGINE_DRIVER_DIR = "Android\\data\\jmri.enginedriver\\files";

    private Menu AMenu;
    private Toolbar toolbar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {        // expedite
            this.finish();
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
        CloseButtonListener closeButtonListener = new CloseButtonListener();
        closeButton.setOnClickListener(closeButtonListener);

//        Button resetButton = findViewById(R.id.logviewer_button_reset);
//        reset_button_listener reset_click_listener = new reset_button_listener();
//        resetButton.setOnClickListener(reset_click_listener);

        saveButton = findViewById(R.id.logviewer_button_save);
        SaveButtonListener saveClickListener = new SaveButtonListener();
        saveButton.setOnClickListener(saveClickListener);

        stopSaveButton = findViewById(R.id.logviewer_button_stop_save);
        StopSaveButtonListener stopSaveClickListener = new StopSaveButtonListener();
        stopSaveButton.setOnClickListener(stopSaveClickListener);

        saveInfoTV = findViewById(R.id.logviewer_info);

        shareButton = findViewById(R.id.logviewer_button_share); // Assuming you added the button with this ID
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainapp.buttonVibration(); // If you want vibration
                showLogFileSelectionDialog();
            }
        });

        showHideSaveButton();

        logReaderTask = new LogReaderTask();

//        logReaderTask.execute();

        //put pointer to this activity's handler in main app's shared variable
        mainapp.logviewer_msg_handler = new logviewer_handler(Looper.getMainLooper());

        LinearLayout screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        LinearLayout statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();

            SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
            String name = prefs.getString("throttle_name_preference", "");

            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name) + " | " + name,
                    getApplicationContext().getResources().getString(R.string.app_name_log_viewer),
                    "");
        }

        Log.d(threaded_application.applicationName, mainapp.getAboutInfo());
        Log.d(threaded_application.applicationName, mainapp.getAboutInfo(false));

    } // end onCreate

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

        threaded_application.currentActivity = activity_id_type.LOG_VIEWER;
        if (mainapp.isForcingFinish()) {        //expedite
            this.finish();
        }

        if (AMenu != null) {
            mainapp.displayFlashlightMenuButton(AMenu);
            mainapp.setFlashlightActionViewButton(AMenu, findViewById(R.id.flashlight_button));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logviewer_menu, menu);
        mainapp.displayEStop(menu);
        AMenu = menu;
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightActionViewButton(menu, findViewById(R.id.flashlight_button));
        mainapp.displayPowerStateMenuButton(menu);
//        mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        if (findViewById(R.id.powerLayoutButton) == null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
                }
            }, 100);
        } else {
            mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        }

        adjustToolbarSize(menu);

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
            mainapp.toggleFlashlightActionView(this, AMenu, findViewById(R.id.flashlight_button));
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
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
        Log.d(threaded_application.applicationName, activityName + ": onDestroy()");

        if (logReaderTask != null ) {
            logReaderTask.stopTask();
        }
        super.onDestroy();
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

//    public class reset_button_listener implements View.OnClickListener {
//        public void onClick(View v) {
//
//        }
//    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        if (key == KEYCODE_BACK) {
            endThisActivity();
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    void endThisActivity() {
        threaded_application.activityInTransition(activityName);
        this.finish();  //end this activity
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    class logviewer_handler extends Handler {

        public logviewer_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateActionViewButton(AMenu, findViewById(R.id.powerLayoutButton));
                        }
                    }
                    break;
                }
                case message_type.LOG_ENTRY_RECEIVED: {
                    String s = msg.obj.toString();
                    addLogEntryToView(s);
                    break;
                }
                case message_type.REOPEN_THROTTLE:
                    endThisActivity();
                    break;

                default:
                    break;
            }
        }
    }

    public class SaveButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            saveLogFile();
        }
    }

    public class StopSaveButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            stopSaveLogFile();
        }
    }

    private void saveLogFile() {
        File logFile = new File(context.getExternalFilesDir(null), "logcat" + System.currentTimeMillis() + ".txt");

        try {
            Runtime.getRuntime().exec("logcat -c");
            mainapp.logcatProcess = Runtime.getRuntime().exec("logcat -f " + logFile);
            threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastSaveLogFile, logFile.toString()), Toast.LENGTH_LONG);
            mainapp.logSaveFilename = logFile.toString();
            showHideSaveButton();
            Log.d(threaded_application.applicationName, "Logging started to: " + logFile);
            Log.d(threaded_application.applicationName, mainapp.getAboutInfo());
            Log.d(threaded_application.applicationName, mainapp.getAboutInfo(false));
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    private void stopSaveLogFile() {
        if (mainapp.logcatProcess != null) {
            mainapp.logcatProcess.destroy(); // Sends SIGTERM to the process
            mainapp.logcatProcess = null;
            Log.d(threaded_application.applicationName, "Logcat file recording stopped.");
            threaded_application.safeToast("Logcat recording stopped.", Toast.LENGTH_SHORT);
            // You might want to update UI or mainapp.logSaveFilename here if needed
            mainapp.logSaveFilename = ""; // Or indicate it's no longer active
            showHideSaveButton();
        }
    }

    void showHideSaveButton() {
        if (!mainapp.logSaveFilename.isEmpty()) {
            saveButton.setVisibility(View.GONE);
            stopSaveButton.setVisibility(View.VISIBLE);
            shareButton.setVisibility(View.GONE);
            saveInfoTV.setText(String.format(getApplicationContext().getResources().getString(R.string.infoSaveLogFile), mainapp.logSaveFilename) );
            saveInfoTV.setVisibility(View.GONE);
        } else {
            saveButton.setVisibility(View.VISIBLE);
            stopSaveButton.setVisibility(View.GONE);
            shareButton.setVisibility((checkHasLogFiles()) ? View.VISIBLE : View.GONE);
            saveInfoTV.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@PermissionsHelper.RequestCodes int requestCode) {
        Log.d(threaded_application.applicationName, activityName + ": navigateToHandler():" + requestCode);
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
//                    Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Got permission for STORE_LOG_FILES - navigate to writeSharedPreferencesToFileImpl()");
//                    saveLogFileImpl();
//                    break;
                default:
                    // do nothing
                    Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Unrecognised permissions request code: " + requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(LogViewerActivity.this, requestCode, permissions, grantResults)) {
            Log.d(threaded_application.applicationName, activityName + ": onRequestPermissionsResult(): Unrecognised request - send up to super class");
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

//        @Override
//        public long getItemId(int position) {
//            return position;
//        }

        @Override
        public String getItem(int position) {
            return ((null != objects) ? objects.get(position) : null);
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;

            if (null == view) {
                LayoutInflater vi = (LayoutInflater) LogViewerActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.logitem, parent, false);
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
                if (!msg.startsWith("<span>")) {
                    msg = msg.replaceAll("&", "&amp;");
                    msg = msg.replaceAll("<", "&lt;");
                    msg = msg.replaceAll(">", "&gt;");
                    if (!msg.contains("About: ")) {
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

    public class LogReaderTask implements Runnable{
        private final String[] LOGCAT_CMD = new String[]{"logcat", "Engine_Driver:D", "*:S"};
        private boolean isRunning;
        private Process logprocess = null;
        private BufferedReader reader = null;

        public LogReaderTask() {
            isRunning = true;
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                logprocess = Runtime.getRuntime().exec(LOGCAT_CMD);
            } catch (IOException e) {
                e.printStackTrace();
                isRunning = false;
            }

            try {
                reader = new BufferedReader(new InputStreamReader(
                        logprocess.getInputStream()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();

                isRunning = false;
            }

            String line;

            try {
                logprocess = Runtime.getRuntime().exec(LOGCAT_CMD);

                while (isRunning) {
                    line = reader.readLine();
                    mainapp.sendMsg(mainapp.logviewer_msg_handler, message_type.LOG_ENTRY_RECEIVED, line);
                }
            } catch (Exception e) {
                isRunning = false;
            }
        }

        public void stopTask() {
            isRunning = false;
        }
    }

    void addLogEntryToView(String line) {
        try {
            adaptor.add(line);
            adaptor.notifyDataSetChanged();
        } catch (Exception e) {
            Log.d(threaded_application.applicationName, activityName + ": addLogEntryToView(): addLine: exception: " + e);
        }
    }

    void adjustToolbarSize(Menu menu) {
        ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
        int toolbarHeight = layoutParams.height;
        int newHeightAndWidth = toolbarHeight;

        if (!threaded_application.useSmallToolbarButtonSize) {
            newHeightAndWidth = toolbarHeight*2;
            layoutParams.height = newHeightAndWidth;
            toolbar.setLayoutParams(layoutParams);
        }
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

    public boolean checkHasLogFiles() {
        mainapp.iplsFileNames = new ArrayList<>();
        mainapp.iplsNames = new ArrayList<>();

        try {
            File dir = new File(context.getExternalFilesDir(null).getPath());
            File[] filesList = dir.listFiles();
            if (filesList != null) {
                for (File file : filesList) {
                    String lowercaseFileName = file.getName().toLowerCase();
                    if ( (lowercaseFileName.startsWith("logcat")) && (lowercaseFileName.endsWith(".txt")) ) {
                        return true; // got one, so just exit
                    }
                }
            }
        } catch (Exception e) {
            Log.d(threaded_application.applicationName, activityName + ": checkHasLogFiles(): Error trying to find log files");
        }

        return false;
    }

    public ArrayList<File> getLogFilesForDialog() {
        ArrayList<File> logFiles = new ArrayList<>();
        try {
            File dir = new File(context.getExternalFilesDir(null).getPath());
            if (dir.exists() && dir.isDirectory()) {
                File[] filesList = dir.listFiles();
                if (filesList != null) {
                    for (File file : filesList) {
                        String lowercaseFileName = file.getName().toLowerCase();
                        if (lowercaseFileName.startsWith("logcat") && lowercaseFileName.endsWith(".txt")) {
                            logFiles.add(file);
                            Log.d(threaded_application.applicationName, activityName + ": getFilesForDialog(): Found: " + file.getName());
                        }
                    }
                    // Optional: Sort the files, e.g., by name or date
                    Collections.sort(logFiles, (file1, file2) -> file2.getName().compareTo(file1.getName())); // Sort descending by name (newest first if using timestamp)
                }
            }
        } catch (Exception e) {
            Log.e(threaded_application.applicationName, activityName + ": getFilesForDialog(): Error trying to find log files", e);
            threaded_application.safeToast("Error accessing log files.", Toast.LENGTH_SHORT);
        }
        return logFiles;
    }

    private void showLogFileSelectionDialog() {
        ArrayList<File> logFiles = getLogFilesForDialog();

        if (logFiles.isEmpty()) return;

        // Extract just the file names for display in the dialog
        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : logFiles) {
            fileNames.add(file.getName());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.file_list_dialog, null);
        builder.setView(dialogView);

        ListView dialogListView = dialogView.findViewById(R.id.file_dialog_listview);
        Button cancelButton = dialogView.findViewById(R.id.file_dialog_button_cancel);

        // --- Setup ListView ---
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this,
                R.layout.file_list_item, // This layout MUST define the font
                R.id.file_list_item_text,    // The ID of the TextView within logfile_list_item.xml
                fileNames);
        dialogListView.setAdapter(listAdapter);

        final AlertDialog dialog = builder.create(); // Create before setting item click listener for ListView

        dialogListView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedFile = logFiles.get(position);
//            threaded_application.safeToast("Selected: " + selectedFile.getName(), Toast.LENGTH_SHORT);
            shareFile(selectedFile,selectedFile.getName());
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void shareFile(File file, String fileName) {
        Uri fileUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                file
        );
        shareFile(fileUri, fileName);
    }
    private void shareFile(Uri fileUri, String fileName) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain"); // Set the MIME type of the file
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Important for granting temporary read access

        // Verify that there are apps available to handle this intent
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, getApplicationContext().getResources().getString(R.string.shareFile, fileName)));
        } else {
            threaded_application.safeToast(R.string.toastNoAppToShare, Toast.LENGTH_SHORT);
        }
    }

}