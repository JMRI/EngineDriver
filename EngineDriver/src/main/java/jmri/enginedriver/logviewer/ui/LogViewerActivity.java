package jmri.enginedriver.logviewer.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import jmri.enginedriver.R;
import jmri.enginedriver.preferences;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.util.PermissionsHelper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//import jmri.enginedriver.logviewer.R;

public class LogViewerActivity extends ListActivity {
    private LogStringAdaptor adaptor = null;
    private LogReaderTask logReaderTask = null;
    private threaded_application mainapp;  // hold pointer to mainapp

    private static final String ENGINE_DRIVER_DIR = "engine_driver";

    public void setTitleToIncludeThrotName() {
        SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
        setTitle(getApplicationContext().getResources().getString(R.string.logViewerTitle,
                prefs.getString("throttle_name_preference", defaultName)));
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        mainapp.applyTheme(this);
        setContentView(R.layout.log_main);

        setTitleToIncludeThrotName();

        ArrayList<String> logarray = new ArrayList<>();
        adaptor = new LogStringAdaptor(this, R.id.txtLogString, logarray);

        setListAdapter(adaptor);

        //Set the buttons
        Button closeButton = findViewById(R.id.logviewer_button_close);
        close_button_listener close_click_listener = new close_button_listener();
        closeButton.setOnClickListener(close_click_listener);

//        Button resetButton = findViewById(R.id.logviewer_button_reset);
//        reset_button_listener reset_click_listener = new reset_button_listener();
//        resetButton.setOnClickListener(reset_click_listener);

        Button saveButton = findViewById(R.id.logviewer_button_save);
        save_button_listener save_click_listener = new save_button_listener();
        saveButton.setOnClickListener(save_click_listener);


        logReaderTask = new LogReaderTask();

        logReaderTask.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) {        //expedite
            this.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logviewer_menu, menu);
        mainapp.displayEStop(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        Log.d("Engine_Driver", "log_viewer.onDestroy() called");

        if (logReaderTask != null ) {
            logReaderTask.stopTask();
        }
        super.onDestroy();
    }

    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            finish();
        }
    }

//    public class reset_button_listener implements View.OnClickListener {
//        public void onClick(View v) {
//
//        }
//    }

    public class save_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            saveLogFile();
        }
    }

    private void saveLogFile() {
        navigateToHandler(PermissionsHelper.STORE_LOG_FILES);
    }

    private void saveLogFileImpl() {
        File path = Environment.getExternalStorageDirectory();
        File engine_driver_dir = new File(path, ENGINE_DRIVER_DIR);
        engine_driver_dir.mkdir();            // create directory if it doesn't exist

        File logFile = new File( engine_driver_dir, "logcat" + System.currentTimeMillis() + ".txt" );
        try {
            Process process = Runtime.getRuntime().exec("logcat -c");
            process = Runtime.getRuntime().exec("logcat -f " + logFile);
//                process = Runtime.getRuntime().exec("logcat -f /engine_driver/log.txt" );
//            String x = getApplicationContext().getResources().getString(R.string.toastSaveLogFile, ENGINE_DRIVER_DIR+ "logcat" + System.currentTimeMillis() + ".txt") ;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastSaveLogFile, ENGINE_DRIVER_DIR+ "logcat" + System.currentTimeMillis() + ".txt"), Toast.LENGTH_LONG).show();
        } catch ( IOException e ) {
            e.printStackTrace();
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
                case PermissionsHelper.STORE_LOG_FILES:
                    Log.d("Engine_Driver", "Preferences: Got permission for STORE_LOG_FILES - navigate to saveSharedPreferencesToFileImpl()");
                    saveLogFileImpl();
                    break;
                default:
                    // do nothing
                    Log.d("Engine_Driver", "Preferences: Unrecognised permissions request code: " + requestCode);
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        final AlertDialog.Builder builder = new AlertDialog.Builder(LogViewerActivity.this);
        String text = ((TextView) v).getText().toString();

        builder.setMessage(text);

        builder.show();
    }

    private class LogStringAdaptor extends ArrayAdapter<String> {
        private List<String> objects;

        public LogStringAdaptor(Context context, int textviewid, List<String> objects) {
            super(context, textviewid, objects);

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
                } else {
                    msgStart = data.indexOf("): "); //pre-marshmallow format
                    if (msgStart > 0) {
                        msg = data.substring(msgStart + 3);
                    }
                }
                textview.setText(msg);
                return view;
            }
            return null;

        }
    }

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

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
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
            }
            //			lastLine = values[0];
        }

        public void stopTask() {
            isRunning = false;
            if (logprocess != null) logprocess.destroy();
        }
    }
}