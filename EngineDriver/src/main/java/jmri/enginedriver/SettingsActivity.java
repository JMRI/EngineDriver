/*Copyright (C) 2018 M. Steve Todd
  mstevetodd@gmail.com

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

The SettingActivity is a replacement for the original preferences activity that
was rewritten to support AppCompat.V7
 */

package jmri.enginedriver;

import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import jmri.enginedriver.util.InPhoneLocoSoundsLoader;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    static public final int RESULT_GAMEPAD = RESULT_FIRST_USER;
    static public final int RESULT_ESUMCII = RESULT_GAMEPAD + 1;
    public static final int RESULT_LOAD_IMG = 1;
    private int result;                     // set to RESULT_FIRST_USER when something is edited

    private String deviceId = "";

    public SharedPreferences prefs;
    public threaded_application mainapp;  // hold pointer to mainapp

    private Toolbar toolbar;
    private Menu SAMenu;

    private String exportedPreferencesFileName = "exported_preferences.ed";
    private boolean overwiteFile = false;

    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    private static final String IMPORT_PREFIX = "Import- "; // these two have to be the same length
    private static final String EXPORT_PREFIX = "Export- ";

    private static final String IMPORT_EXPORT_OPTION_NONE = "None";
    private static final String IMPORT_EXPORT_OPTION_EXPORT = "Export";
    private static final String IMPORT_EXPORT_OPTION_IMPORT = "Import";
    private static final String IMPORT_EXPORT_OPTION_RESET = "Reset";
    private static final String IMPORT_EXPORT_OPTION_IMPORT_URL = "URL";

    private static String AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT = "Connect Disconnect";

    private static final String EXTERNAL_URL_PREFERENCES_IMPORT = "external_url_preferences_import.ed";
    private static final String ENGINE_DRIVER_DIR = "Android/data/jmri.enginedriver/files";
    private static final String SERVER_ENGINE_DRIVER_DIR = "prefs/engine_driver";

    private static final String DEMO_HOST = "jmri.mstevetodd.com";
    private String[] prefHostImportExportEntriesFound = {"None"};
    private String[] prefHostImportExportEntryValuesFound = {"None"};

    private ProgressDialog pDialog;
    public static final int PROGRESS_BAR_TYPE = 0;

    private static String GAMEPAD_BUTTON_NOT_AVAILABLE_LABEL = "Button not available";
    private static String GAMEPAD_BUTTON_NOT_USABLE_LABEL = "Button not usable";

    private static final String PREF_IMPORT_ALL_FULL = "Yes";
    private static final String PREF_IMPORT_ALL_PARTIAL = "No";
    private static final String PREF_IMPORT_ALL_RESET = "-";

    private boolean forceRestartAppOnPreferencesClose = false;
    private int forceRestartAppOnPreferencesCloseReason = 0;
    private boolean forceReLaunchAppOnPreferencesClose = false;

    private boolean isInSubScreen = false;

    private String prefThrottleScreenType = "Default";
    private String prefThrottleScreenTypeOriginal = "Default";
    protected boolean prefBackgroundImage = false;
    boolean prefThrottleSwitchButtonDisplay = false;
    protected boolean prefHideSlider = false;

    private String prefConsistFollowRuleStyle = "original";
    private static final String CONSIST_FUNCTION_RULE_STYLE_ORIGINAL = "original";
    private static final String CONSIST_FUNCTION_RULE_STYLE_COMPLEX = "complex";
    private static final String CONSIST_FUNCTION_RULE_STYLE_SPECIAL_EXACT = "specialExact";
    private static final String CONSIST_FUNCTION_RULE_STYLE_SPECIAL_PARTIAL = "specialPartial";

    private boolean ignoreThisThrottleNumChange = false;

    private static String[] deviceSoundsEntryValuesArray;
    private static String[] deviceSoundsEntriesArray; // display version
    public static InPhoneLocoSoundsLoader iplsLoader;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Engine_Driver", "Settings: onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment;
        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragment = new SettingsFragment().newInstance("Advanced Setting");
            fragmentTransaction.add(R.id.settings_preferences_frame, fragment);
            fragmentTransaction.commit();
        }

        mainapp = (threaded_application) this.getApplication();
        mainapp.applyTheme(this,true);


        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        deviceId = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        //put pointer to this activity's message handler in main app's shared variable (If needed)
        mainapp.settings_msg_handler = new SettingsActivity.settings_handler();

        //put pointer to this activity's message handler in main app's shared variable (If needed)
        mainapp.preferences_msg_handler = new SettingsActivity.settings_handler();

        iplsLoader = new InPhoneLocoSoundsLoader(mainapp, prefs, context);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_preferences),
                    "");
            Log.d("Engine_Driver", "Settings: Set toolbar");
        }

    } // end onCreate

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen) {
        Log.d("Engine_Driver", "callback called to attach the preference sub screen");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SettingsSubScreenFragment fragment = SettingsSubScreenFragment.newInstance("Advanced Settings Subscreen");
        Bundle args = new Bundle();
        //Defining the sub screen as new root for the  subscreen
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.replace(R.id.settings_preferences_frame, fragment, preferenceScreen.getKey());
//        ft.addToBackStack(null);
        ft.addToBackStack(preferenceScreen.getKey());
        ft.commit();

        return true;
    }

    @Override
    protected void onResume() {
        Log.d("Engine_Driver", "Settings: onResume()");
        super.onResume();

        Log.d("Engine_Driver", "settings.onResume() called");
        try {
            dismissDialog(PROGRESS_BAR_TYPE);
        } catch (Exception e) {
            Log.d("Engine_Driver", "settings.onResume() no dialog to kill");
        }

        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        if (SAMenu != null) {
            mainapp.displayEStop(SAMenu);
            mainapp.displayFlashlightMenuButton(SAMenu);
            mainapp.setFlashlightButton(SAMenu);
            mainapp.displayPowerStateMenuButton(SAMenu);
            mainapp.setPowerStateButton(SAMenu);
        }

//        mainapp.applyTheme(this,true);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_preferences),
                    "");
            Log.d("Engine_Driver", "Settings: Set toolbar");
        }
    }

//    @Override
//    protected void onStart() {
//        Log.d("Engine_Driver", "Settings: onStart()");
//        super.onStart();
//    }

    @Override
    protected void onDestroy() {
        Log.d("Engine_Driver", "settings.onDestroy() called");
        super.onDestroy();
        if (mainapp.settings_msg_handler !=null) {
            mainapp.settings_msg_handler.removeCallbacksAndMessages(null);
            mainapp.settings_msg_handler = null;
        } else {
            Log.d("Engine_Driver", "Preferences: onDestroy: mainapp.preferences_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
        if (forceRestartAppOnPreferencesClose) {
            forceRestartApp(forceRestartAppOnPreferencesCloseReason);
        }
        if (forceReLaunchAppOnPreferencesClose) {
            forceReLaunchApp(forceRestartAppOnPreferencesCloseReason);
        }
    }

    @SuppressLint("ApplySharedPref")
    public void forceRestartApp(int forcedRestartReason) {
        Log.d("Engine_Driver", "Settings: forceRestartApp() - forcedRestartReason: " + forcedRestartReason);

        String prefAutoImportExport = prefs.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue));

        if (prefAutoImportExport.equals(AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT)) {
            if (mainapp.connectedHostName != null) {
                String exportedPreferencesFileName = mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                saveSharedPreferencesToFile(prefs, exportedPreferencesFileName, false);
            }
        }
        finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

    @SuppressLint("ApplySharedPref")
    public void forceReLaunchApp(int forcedRestartReason) {
        Log.d("Engine_Driver", "Settings: forceRelaunchApp() ");

        finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        Message msg = Message.obtain();
        msg.what = message_type.RELAUNCH_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);

    }

    @SuppressLint("ApplySharedPref")
    public void start_gamepad_test_activity() {

        boolean result = prefs.getBoolean("prefGamepadTestNow", getResources().getBoolean(R.bool.prefGamepadTestNowDefaultValue));

        if (result) {
            prefs.edit().putBoolean("prefGamepadTestNow", false).commit();  //reset the preference
            this.reload();

            try {
                Intent in = new Intent().setClass(this, gamepad_test.class);
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d("Engine_Driver", "Settings: " + ex.getMessage());
            }
        }
    }

    public void reload() {
        // restart the activity so all the preferences show correctly based on what was imported / hidden
        Log.d("Engine_Driver", "Settings: Forcing activity to recreate");
        recreate();
    }

    @SuppressLint("ApplySharedPref")
    private boolean saveSharedPreferencesToFile(SharedPreferences sharedPreferences, String exportedPreferencesFileName, boolean confirmDialog) {
        Log.d("Engine_Driver", "Settings: Saving preferences to file");
        sharedPreferences.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        boolean res = false;
        if (!exportedPreferencesFileName.equals(".ed")) {
//            File path = Environment.getExternalStorageDirectory();
//            File engine_driver_dir = new File(path, ENGINE_DRIVER_DIR);
//            engine_driver_dir.mkdir();            // create directory if it doesn't exist
//            File dst = new File(path, ENGINE_DRIVER_DIR + "/" + exportedPreferencesFileName);
            File dst = new File(context.getExternalFilesDir(null), exportedPreferencesFileName);

            if ((dst.exists()) && (confirmDialog)) {
                overwiteFileDialog(sharedPreferences, ENGINE_DRIVER_DIR + "/" + exportedPreferencesFileName);
            } else {
                res = importExportPreferences.saveSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.prefImportExportErrorNotConnected, Toast.LENGTH_LONG).show();
        }
        return res;
    }

    public boolean overwiteFileDialog(final SharedPreferences sharedPreferences, String fileNameAndPath) {
        boolean res = false;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                boolean res = false;
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        res = importExportPreferences.saveSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
                        overwiteFile = true;
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        overwiteFile = false;
                        break;
                }
                fixAndReloadImportExportPreference(sharedPreferences);
                mainapp.buttonVibration();
            }
        };
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setMessage(getApplicationContext().getResources().getString(R.string.prefImportExportOverwite).replace("%1$s",fileNameAndPath))
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener);
        ab.show();
        return overwiteFile;
    }

    @SuppressLint("ApplySharedPref")
    public void fixAndReloadImportExportPreference(SharedPreferences sharedPreferences) {
        Log.d("Engine_Driver", "Settings: Fix and Loading saved preferences.");
        sharedPreferences.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        sharedPreferences.edit().putString("prefHostImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        reload();
    }

    private void loadSharedPreferencesFromFileDialog(final SharedPreferences sharedPreferences, final String exportedPreferencesFileName, final String deviceId, final int forceRestartReason) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        loadSharedPreferencesFromFile(sharedPreferences, exportedPreferencesFileName, deviceId, forceRestartReason);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        sharedPreferences.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
                        reload();
                        break;
                }
                mainapp.buttonVibration();
            }
        };

        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle(getApplicationContext().getResources().getString(R.string.dialogConfirmImportPreferencesTitle))
                .setMessage(getApplicationContext().getResources().getString(R.string.dialogImportPreferencesQuestion))
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener);
        ab.show();
    }


    private boolean loadSharedPreferencesFromFile(SharedPreferences sharedPreferences, String exportedPreferencesFileName, String deviceId, int forceRestartReason) {
        Log.d("Engine_Driver", "Settings: Loading saved preferences from file: " + exportedPreferencesFileName);
        boolean res = importExportPreferences.loadSharedPreferencesFromFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName, deviceId, false);

        if (!res) {
            Toast.makeText(getApplicationContext(),
                    getApplicationContext().getResources().getString(R.string.prefImportExportErrorReadingFrom, exportedPreferencesFileName),
                    Toast.LENGTH_LONG).show();
        }
        forceRestartApp(forceRestartReason);
        return res;
    }

    private void resetPreferencesDialog() {
        Log.d("Engine_Driver", "Settings: Resetting preferences");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        resetPreferences();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        prefs.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
                        reload();
                        break;
                }
            }
        };

        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle(getApplicationContext().getResources().getString(R.string.dialogConfirmResetPreferencesTitle))
                .setMessage(getApplicationContext().getResources().getString(R.string.dialogResetPreferencesQuestion))
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener);
        ab.show();
    }

    @SuppressLint("ApplySharedPref")
    private void resetPreferences() {
        SharedPreferences.Editor prefEdit = prefs.edit();
        prefEdit.clear();
        prefEdit.commit();
        Log.d("Engine_Driver", "Settings: Reset succeeded");
        delete_settings_file("function_settings.txt");
        delete_settings_file("connections_list.txt");
        delete_settings_file("recent_engine_list.txt");
        delete_auto_import_settings_files();

        reload();

        forceRestartApp(mainapp.FORCED_RESTART_REASON_RESET);
    }

    private void delete_auto_import_settings_files() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            File sdcard_path = Environment.getExternalStorageDirectory();
//            File dir = new File(sdcard_path, ENGINE_DRIVER_DIR); // in case the folder does not already exist
            File dir = new File(context.getExternalFilesDir(null), ENGINE_DRIVER_DIR);
            File[] edFiles = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File folder, String name) {
                    return name.toLowerCase().startsWith("auto_");
                }
            });
            if (edFiles != null && edFiles.length > 0){
                for (int i=0; i<edFiles.length; i++) {
                    delete_settings_file(edFiles[i].getName());
                }
            }
        }
    }

    private void delete_settings_file(String file_name) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            File sdcard_path = Environment.getExternalStorageDirectory();
//            File settings_file = new File(sdcard_path, "engine_driver/" + file_name);
            File settings_file = new File(context.getExternalFilesDir(null), file_name);
            if (settings_file.exists()) {
                if (settings_file.delete()) {
                    Log.d("Engine_Driver", "Settings: " + file_name + " deleted");
                } else {
                    Log.e("Engine_Driver", "Settings: " + file_name + " NOT deleted");
                }
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    //Handle messages from the communication thread back to the UI thread.
    // currently only for the download from a URL
    @SuppressLint("HandlerLeak")
    private class settings_handler extends Handler {

        @SuppressLint("ApplySharedPref")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE:                       // see if loco added to or removed from any throttle
                    String response_str = msg.obj.toString();
                    if (response_str.length() >= 3) {
                        char com1 = response_str.charAt(0);
                        char com2 = response_str.charAt(2);

                        String comA = response_str.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(comA)) {
                            mainapp.setPowerStateButton(SAMenu);
                        }
                    }
                    break;
                case message_type.IMPORT_SERVER_MANUAL_SUCCESS:
                    Log.d("Engine_Driver", "Settings: Message: Import preferences from Server: File Found");
                    loadSharedPreferencesFromFile(prefs, EXTERNAL_URL_PREFERENCES_IMPORT, deviceId, mainapp.FORCED_RESTART_REASON_IMPORT_SERVER_MANUAL);
                    break;
                case message_type.IMPORT_SERVER_MANUAL_FAIL:
                    Log.d("Engine_Driver", "Settings: Message: Import preferences from Server: File not Found");
                    prefs.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
                    prefs.edit().putString("prefHostImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
                    Toast.makeText(getApplicationContext(),
                            getApplicationContext().getResources().getString(R.string.toastPreferencesImportServerManualFailed,
                            prefs.getString("prefImportServerManual", getApplicationContext().getResources().getString(R.string.prefImportServerManualDefaultValue))), Toast.LENGTH_LONG).show();
                    reload();
                    break;

            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) { // dialog for the progress bar
        //noinspection SwitchStatementWithTooFewBranches
        switch (id) {
            case PROGRESS_BAR_TYPE: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    class importFromURL extends AsyncTask<String, String, String> {   // Background Async Task to download file

        //Before starting background thread Show Progress Bar Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(PROGRESS_BAR_TYPE);
        }

        // Importing file in background thread
        @SuppressLint("ApplySharedPref")
        @Override
        protected String doInBackground(String... f_url) {
            Log.d("Engine_Driver", "Settings: Import preferences from Server: start");
            int count;
            String n_url = f_url[0].trim();

            if ((mainapp.connectedHostip != null)) {
                n_url = "http://" + mainapp.connectedHostip + ":" + mainapp.web_server_port + "/" + SERVER_ENGINE_DRIVER_DIR + "/" + f_url[0];
            } else {
                Log.d("Engine_Driver", "Settings: Import preferences from Server: Not currently connected");
                return null;
            }

            try {
                URL url = new URL(n_url);
                URLConnection conection = url.openConnection();
                conection.connect();

                // to help show a 0-100% progress bar
                int lengthOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

//                File Directory = new File(ENGINE_DRIVER_DIR); // in case the folder does not already exist

                // Output stream
//                FileOutputStream output = new FileOutputStream(Environment
//                        .getExternalStorageDirectory().toString()
//                        + "/" + ENGINE_DRIVER_DIR + "/" + EXTERNAL_URL_PREFERENCES_IMPORT);
                FileOutputStream output = new FileOutputStream(context.getExternalFilesDir(null)
                        + "/" + EXTERNAL_URL_PREFERENCES_IMPORT);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

                dismissDialog(PROGRESS_BAR_TYPE);
                prefs.edit().putString("prefPreferencesImportFileName", n_url).commit();
                mainapp.sendMsgDelay(mainapp.settings_msg_handler, 1000L, message_type.IMPORT_SERVER_MANUAL_SUCCESS);

            } catch (Exception e) {
                Log.e("Engine_Driver", "Settings: Import preferences from Server Failed: " + e.getMessage());
                try {
                    dismissDialog(PROGRESS_BAR_TYPE);
                } catch (Exception ignored) {
                }
                prefs.edit().putString("prefPreferencesImportAll", PREF_IMPORT_ALL_RESET).commit();
                mainapp.sendMsgDelay(mainapp.settings_msg_handler, 1000L, message_type.IMPORT_SERVER_MANUAL_FAIL);
            }

            Log.d("Engine_Driver", "Settings: Import preferences from Server: End");
            return null;
        }

        protected void onProgressUpdate(String... progress) {  //update progress bar
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(PROGRESS_BAR_TYPE);

        }

    }

    protected void loadImagefromGallery() {
//        if (PermissionsHelper.getInstance().isPermissionGranted(this, PermissionsHelper.READ_PREFERENCES)) {
//            loadImagefromGalleryImpl();
//        }
//    }
//
//    public void loadImagefromGalleryImpl() {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if ((key == KeyEvent.KEYCODE_BACK) && (!isInSubScreen) ) {
            setResult(result);
            finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        isInSubScreen = false;
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("Engine_Driver", "Settings: onCreateOptionsMenu()");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        SAMenu = menu;
        mainapp.displayEStop(menu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                mainapp.buttonVibration();
                return true;
            case R.id.flashlight_button:
                mainapp.toggleFlashlight(this, SAMenu);
                mainapp.buttonVibration();
                return true;
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    mainapp.powerControlNotAllowedDialog(SAMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                mainapp.buttonVibration();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Engine_Driver", "Settings: onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.MediaColumns.DATA };

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imgpath = cursor.getString(columnIndex);
                cursor.close();

                SharedPreferences.Editor edit=prefs.edit();
                edit.putString("prefBackgroundImageFileName",imgpath);
                edit.commit();

                forceRestartAppOnPreferencesClose = true;
                forceRestartAppOnPreferencesCloseReason = mainapp.FORCED_RESTART_REASON_BACKGROUND;
            }
            else {
                Toast.makeText(this, R.string.prefBackgroundImageFileNameNoImageSelected, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("Engine_Driver", "Settings: Loading background image Failed: " + e.getMessage());
        }

    }

    @SuppressLint("ApplySharedPref")
    protected boolean limitIntPrefValue(PreferenceScreen prefScreen, SharedPreferences sharedPreferences, String key, int minVal, int maxVal, String defaultVal) {
        Log.d("Engine_Driver", "Settings: limitIntPrefValue()");
        boolean isValid = true;
        EditTextPreference prefText = (EditTextPreference) prefScreen.findPreference(key);
        try {
            int newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Integer.toString(maxVal)).commit();
                prefText.setText(Integer.toString(maxVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Integer.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Integer.toString(minVal)).commit();
                prefText.setText(Integer.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Integer.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric, Integer.toString(minVal), Integer.toString(maxVal), defaultVal), Toast.LENGTH_LONG).show();
        }
        return isValid;
    }

    @SuppressLint("ApplySharedPref")
    protected boolean limitFloatPrefValue(PreferenceScreen prefScreen, SharedPreferences sharedPreferences, String key, Float minVal, Float maxVal, String defaultVal) {
        Log.d("Engine_Driver", "Settings: limitFloatPrefValue()");
        boolean isValid = true;
        EditTextPreference prefText = (EditTextPreference) prefScreen.findPreference(key);
        try {
            Float newVal = Float.parseFloat(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Float.toString(maxVal)).commit();
                prefText.setText(Float.toString(maxVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Float.toString(minVal), Float.toString(maxVal), Float.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Float.toString(minVal)).commit();
                prefText.setText(Float.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Float.toString(minVal), Float.toString(maxVal), Float.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric, Float.toString(minVal), Float.toString(maxVal), defaultVal), Toast.LENGTH_LONG).show();
        }
        return isValid;
    }

    @SuppressLint("ApplySharedPref")
    public void checkThrottleScreenType(SharedPreferences sharedPreferences) {
        Log.d("Engine_Driver", "Settings: checkThrottleScreenType()");
        prefThrottleScreenType = sharedPreferences.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        if (!prefThrottleScreenType.equals(prefThrottleScreenTypeOriginal)) {
            SharedPreferences.Editor prefEdit = sharedPreferences.edit();
            prefEdit.commit();
            forceRestartAppOnPreferencesClose = true;
            forceRestartAppOnPreferencesCloseReason =  mainapp.FORCED_RESTART_REASON_THROTTLE_SWITCH;
        }
    }

    @SuppressLint("ApplySharedPref")
    public void limitNumThrottles(PreferenceScreen prefScreen, SharedPreferences sharedPreferences) {
        Log.d("Engine_Driver", "Settings: limitNumThrottles()");
        int numThrottles = mainapp.Numeralise(sharedPreferences.getString("NumThrottle", getResources().getString(R.string.NumThrottleDefaulValue)));
        prefThrottleScreenType = sharedPreferences.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        int index = getThrottleScreenTypeArrayIndex(sharedPreferences);
        String[] textNumbers = this.getResources().getStringArray(R.array.NumOfThrottlesEntryValues);
        int[] fixed = this.getResources().getIntArray(R.array.prefThrottleScreenTypeFixedThrottleNumber);
        int[] max = this.getResources().getIntArray(R.array.prefThrottleScreenTypeMaxThrottleNumber);

        if (index < 0) return; //bail if no matches

        if ( ((fixed[index] == 1) && (numThrottles != max[index]))
                || ((fixed[index] == 0) && (numThrottles > max[index])) ) {
            Log.d("Engine_Driver", "Settings: limitNumThrottles: numThrottles " +  numThrottles + " fixed " + fixed[index] + " max " + max[index]);

            sharedPreferences.edit().putString("NumThrottle", textNumbers[max[index]-1]).commit();
            if (numThrottles > max[index]-1) { // only display the warning if the requested amount is lower than the max or fixed.
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastNumThrottles, textNumbers[max[index] - 1]), Toast.LENGTH_LONG).show();
            }
            ListPreference p = (ListPreference) prefScreen.findPreference("NumThrottle");
            if (p != null) {
                ignoreThisThrottleNumChange = true;
                Log.d("Engine_Driver", "Settings: limitNumThrottles: textNumbers[max[index]-1]: " +  textNumbers[max[index]-1] + " index: " + index);
                p.setValue(textNumbers[max[index]-1]);
                p.setValueIndex(max[index]-1);
            }
        }
    }

    boolean throttleScreenTypeSupportsWebView(SharedPreferences sharedPreferences) {
        Log.d("Engine_Driver", "Settings: throttleScreenTypeSupportsWebView()");
        prefThrottleScreenType = sharedPreferences.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        boolean supportsWebView = false;

        int index = getThrottleScreenTypeArrayIndex(sharedPreferences);
        int[] supportWebView = this.getResources().getIntArray(R.array.prefThrottleScreenTypeSupportsWebView);

        if (index < 0) return supportsWebView; //bail if no matches

        if (supportWebView[index] == 0) {
            supportsWebView = true;
        }
        return supportsWebView;
    }

    int getThrottleScreenTypeArrayIndex(SharedPreferences sharedPreferences) {
        Log.d("Engine_Driver", "Settings: getThrottleScreenTypeArrayIndex()");
        prefThrottleScreenType = sharedPreferences.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        int index = -1;
        String[] arr = this.getResources().getStringArray(R.array.prefThrottleScreenTypeEntryValues);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(prefThrottleScreenType)) {
                index = i;
                break;
            }
        }

        return index;
    }

    private void setGamePadPrefLabels(PreferenceScreen prefScreen, SharedPreferences sharedPreferences) {
        String prefGamePadType = sharedPreferences.getString("prefGamePadType", "None").trim();
        String[] gamePadPrefLabels;
        String[] gamePadPrefButtonReferences = this.getResources().getStringArray(R.array.prefGamePadPrefButtonReferences);

        switch (prefGamePadType) {
            case "iCade+DPAD":
            case "iCade+DPAD-rotate":
            case "MTK":
            case "MTK-rotate":
            case "Game":
            case "Game-rotate":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadMocuteLabels);
                break;
/*
            case "VRBoxA":
            case "VRBoxA-rotate":
            case "VRBoxC":
            case "VRBoxC-rotate":
            case "VRBoxiC":
            case "VRBoxiC-rotate":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadVRBoxLabels);
                break;
*/
            case "MagicseeR1B":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadMagicseeR1Labels);
                break;
            case "FlydigiWee2":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadFlydigiWee2Labels);
                break;
            case "UtopiaC":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadUtopiaCLabels);
                break;
            case "Generic":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadGenericLabels);
                break;
            case "Generic3x4":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadGeneric3x4Labels);
                break;
            case "Volume":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadVolumeLabels);
                break;
            case "None":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadNoneLabels);
                break;
            default:
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadMocuteLabels);
                break;
        }
        for (int i = 1; i < gamePadPrefLabels.length; i++) {  // skip the first one
            boolean thisEnabled = true;
            Preference thisPref = (Preference) prefScreen.findPreference(gamePadPrefButtonReferences[i]);
            if (thisPref != null) {
                thisPref.setTitle(gamePadPrefLabels[i]);
                if ((gamePadPrefLabels[i].equals(GAMEPAD_BUTTON_NOT_AVAILABLE_LABEL)) || (gamePadPrefLabels[i].equals(GAMEPAD_BUTTON_NOT_USABLE_LABEL))) {
                    thisEnabled = false;
                }
                thisPref.setSelectable(thisEnabled);
                thisPref.setEnabled(thisEnabled);
            }
        }

        boolean thisEnabled = true;
        if (prefGamePadType.equals("None")) {
            thisEnabled = false;
        }
        Preference thisPref;

        enableDisablePreference(prefScreen, "prefGamePadFeedbackVolume", thisEnabled);
        enableDisablePreference(prefScreen, "prefGamePadSpeedArrowsThrottleRepeatDelay", thisEnabled);
        enableDisablePreference(prefScreen, "prefGamepadprefGamePadDoublePressStop", thisEnabled);
        enableDisablePreference(prefScreen, "prefGamepadSwapForwardReverseWithScreenButtons", thisEnabled);
        enableDisablePreference(prefScreen, "prefGamepadTestEnforceTesting", thisEnabled);
        enableDisablePreference(prefScreen, "prefGamepadTestNow", thisEnabled);
        enableDisablePreference(prefScreen, "prefGamePadSpeedButtonsSpeedStep", thisEnabled);
        enableDisablePreference(prefScreen, "prefGamepadTestEnforceTestingSimple", thisEnabled);

    }

    private void enableDisablePreference(PreferenceScreen prefScreen, String key, boolean enable) {
        Log.d("Engine_Driver", "Settings: enableDisablePreference(): key: " + key);
        Preference p = prefScreen.findPreference(key);
        if (p != null) {
            p.setSelectable(enable);
            p.setEnabled(enable);
        } else {
            Log.w("Engine_Driver", "Preference key '" + key + "' not found, not set to " + enable);
        }
    }

    private void showHideBackgroundImagePreferences(PreferenceScreen prefScreen) {
        boolean enable = true;
        if (prefBackgroundImage) {
            enable = false;
        }
        enableDisablePreference(prefScreen, "prefBackgroundImageFileNameImagePicker", !enable);
        enableDisablePreference(prefScreen, "prefBackgroundImagePosition", !enable);
    }

    private void showHideWebSwipePreferences(PreferenceScreen prefScreen) {
        boolean enable = true;
        String currentValue = prefs.getString("ThrottleOrientation", "");
        if (!currentValue.equals("Auto-Web")) {
            enable = false;
        }
        enableDisablePreference(prefScreen, "swipe_through_web_preference", !enable);
    }

    private void showHideTTSPreferences(PreferenceScreen prefScreen) {
        boolean enable = true;
        String currentValue = prefs.getString("prefTtsWhen", "");
        if (!currentValue.equals("None")) {
            enable = false;
        }

        enableDisablePreference(prefScreen, "prefTtsThrottleResponse", !enable);
        enableDisablePreference(prefScreen, "prefTtsThrottleSpeed", !enable);
        enableDisablePreference(prefScreen, "prefTtsGamepadTest", !enable);
        enableDisablePreference(prefScreen, "prefTtsGamepadTestComplete", !enable);
        enableDisablePreference(prefScreen, "prefTtsGamepadTestKeys", !enable);

    }

    private void showHideConsistRuleStylePreferences(PreferenceScreen prefScreen) {
        boolean enable = false;
        if (prefConsistFollowRuleStyle.equals(CONSIST_FUNCTION_RULE_STYLE_ORIGINAL)) {
            enable = true;
        }

        enableDisablePreference(prefScreen, "SelectiveLeadSound", enable);
        enableDisablePreference(prefScreen, "SelectiveLeadSoundF1", enable);
        enableDisablePreference(prefScreen, "SelectiveLeadSoundF2", enable);

        enable = false;
        if (prefConsistFollowRuleStyle.equals(CONSIST_FUNCTION_RULE_STYLE_COMPLEX)) {
            enable = true;
        }

        enableDisablePreference(prefScreen, "prefConsistFollowDefaultAction", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowString1", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowAction1", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowString2", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowAction2", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowString3", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowAction3", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowString4", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowAction4", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowString5", enable);
        enableDisablePreference(prefScreen, "prefConsistFollowAction5", enable);

    }

    private void showHideThrottleSwitchPreferences(PreferenceScreen prefScreen) {
        Log.d("Engine_Driver", "Settings: showHideThrottleSwitchPreferences()");
        prefThrottleScreenType = prefs.getString("prefThrottleScreenType",
                getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        boolean enable = prefThrottleScreenType.equals("Simple");

        enableDisablePreference(prefScreen, "prefSimpleThrottleLayoutShowFunctionButtonCount", enable);
    }

    public void loadSharedPreferences(){
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
    }

    private void getConnectionsList() {
        boolean foundDemoHost = false;
        String host_name;
        String host_name_filename;
        String errMsg;

        try {
//                File sdcard_path = Environment.getExternalStorageDirectory();
//                File connections_list_file = new File(sdcard_path, "engine_driver/connections_list.txt");
            File connections_list_file = new File(context.getExternalFilesDir(null), "connections_list.txt");

            if (connections_list_file.exists()) {
                BufferedReader list_reader = new BufferedReader(new FileReader(connections_list_file));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    List<String> parts = Arrays.asList(line.split(":", 3)); //split record from file, max of 3 parts
                    if (parts.size() > 1) {  //skip if not split
                        host_name = parts.get(0);
                        host_name_filename = host_name.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                        if (host_name.equals(DEMO_HOST)) {
                            foundDemoHost = true;
                        }
                        if ((!host_name.equals("")) && (!isAlreadyInArray(prefHostImportExportEntriesFound, IMPORT_PREFIX + host_name_filename))) {
                            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, IMPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, EXPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, IMPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, EXPORT_PREFIX + host_name_filename);
                        }
                    }
                }
                list_reader.close();
            } else {
                Log.d("settingActivity", "getConnectionsList: Recent connections not found");
            }
        } catch (IOException except) {
            errMsg = except.getMessage();
            Log.e("Engine_Driver", "Settings: Error reading recent connections list: " + errMsg);
            Toast.makeText(getApplicationContext(), R.string.prefImportExportErrorReadingList + " " + errMsg, Toast.LENGTH_SHORT).show();
        }

        if (!foundDemoHost) {
            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, IMPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, EXPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, IMPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, EXPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
        }

    }

    private static String[] add(String[] stringArray, String newValue) {
        String[] tempArray = new String[stringArray.length + 1];
        System.arraycopy(stringArray, 0, tempArray, 0, stringArray.length);
        tempArray[stringArray.length] = newValue;
        return tempArray;
    }

    public static boolean isAlreadyInArray(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue))
                return true;
        }
        return false;
    }


    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        static public final int RESULT_GAMEPAD = RESULT_FIRST_USER;
        static public final int RESULT_ESUMCII = RESULT_GAMEPAD + 1;

        private threaded_application mainapp;  // hold pointer to mainapp
        private SharedPreferences prefs;

//        private int result;                     // set to RESULT_FIRST_USER when something is edited

//        private static final String DEMO_HOST = "jmri.mstevetodd.com";
//        private String[] prefHostImportExportEntriesFound = {"None"};
//        private String[] prefHostImportExportEntryValuesFound = {"None"};

        private static String GAMEPAD_BUTTON_NOT_AVAILABLE_LABEL = "Button not available";
        private static String GAMEPAD_BUTTON_NOT_USABLE_LABEL = "Button not usable";

        private String prefThemeOriginal = "Default";

        private static final String PREF_IMPORT_ALL_FULL = "Yes";
        private static final String PREF_IMPORT_ALL_PARTIAL = "No";
        private static final String PREF_IMPORT_ALL_RESET = "-";

        public String[] advancedPreferences;

        public static final int RESULT_LOAD_IMG = 1;

        protected String defaultName;
        SettingsActivity parentActivity;

        private static final String TAG = SettingsFragment.class.getName();
        public static final String PAGE_ID = "page_id";
        public static final String FRAGMENT_TAG = "my_preference_fragment";

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        public static SettingsFragment newInstance(String pageId) {
            SettingsFragment f = new SettingsFragment();
            Bundle args = new Bundle();
            args.putString(PAGE_ID, pageId);
            f.setArguments(args);
            return (f);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Log.d("Engine_Driver", "Settings: SettingsFragment onCreatePreferences()");
                setPreferencesFromResource(R.xml.preferences, rootKey);

            Activity a = getActivity();
//            if(a instanceof SettingsActivity) {
                parentActivity = (SettingsActivity) a;
//            }

            setPreferencesUI();
        }

        @Override
        public void onResume() {
            Log.d("Engine_Driver", "Settings: SettingsFragment onResume()");
            super.onResume();

            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);

            setPreferencesUI();

        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            super.onPreferenceTreeClick(preference);
            return false;
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @SuppressLint("ApplySharedPref")
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d("Engine_Driver", "Settings: onSharedPreferenceChanged(): key: " + key);
            boolean prefForcedRestart = sharedPreferences.getBoolean("prefForcedRestart", false);

            if (!prefForcedRestart) {  // don't do anything if the preference have been loaded and we are about to reload the app.
                switch (key) {
                    case "throttle_name_preference": {
                        String currentValue = parentActivity.mainapp.fixThrottleName(sharedPreferences.getString(key, defaultName).trim());
                        break;
                    }
                    case "maximum_throttle_preference":
                        //limit new value to 1-100 (%)
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 1, 100, "100");
                        break;
                    case "maximum_throttle_change_preference":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 1, 100, "25");
                        break;
                    case "speed_arrows_throttle_speed_step":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 1, 99, "4");
                        break;
                    case "prefScreenBrightnessDim":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 1, 100, "5");
                        break;
                    case "prefConnectTimeoutMs":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 100, 99999, getResources().getString(R.string.prefConnectTimeoutMsDefaultValue));
                        break;
                    case "prefSocketTimeoutMs":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 100, 9999, getResources().getString(R.string.prefSocketTimeoutMsDefaultValue));
                        break;
                    case "prefHeartbeatResponseFactor":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 50, 90, getResources().getString(R.string.prefHeartbeatResponseFactorDefaultValue));
                        break;
                    case "prefDeviceSounds0":
                        mainapp.prefDeviceSounds[0] = prefs.getString("prefDeviceSounds0", parentActivity.getApplicationContext().getResources().getString(R.string.prefDeviceSoundsDefaultValue));
                    case "prefDeviceSounds1":
                        mainapp.prefDeviceSounds[1] = prefs.getString("prefDeviceSounds1", parentActivity.getApplicationContext().getResources().getString(R.string.prefDeviceSoundsDefaultValue));
                        mainapp.soundsReloadSounds = true;
                        iplsLoader.loadSounds();
                        break;
                    case "prefDeviceSoundsLocoVolume":
                    case "prefDeviceSoundsBellVolume":
                    case "prefDeviceSoundsHornVolume":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 1, 100, "100");
                        break;
                    case "prefDeviceSoundsMomentum":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 0, 2000, "1000");
                        break;
                    case "ThrottleOrientation":
                        setSwipeThroughWebPreference(); //disable web swipe if Auto-Web
                        //if mode was fixed (Port or Land) won't get callback so need explicit call here
                        parentActivity.mainapp.setActivityOrientation(parentActivity);
                        break;
                    case "InitialWebPage":
                        parentActivity.mainapp.alert_activities(message_type.INITIAL_WEB_WEBPAGE, "");
                        break;
                    case "ClockDisplayTypePreference":
                        parentActivity.mainapp.sendMsg(parentActivity.mainapp.comm_msg_handler, message_type.CLOCK_DISPLAY_CHANGED);
                        break;
                    case "prefNumberOfDefaultFunctionLabels":
                        // limit check new value
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 0, 29, "29");
                        parentActivity.mainapp.set_default_function_labels(false);
                        break;

                    case "prefLocale":
                        sharedPreferences.edit().putString("prefLeftDirectionButtons", "").commit();
                        sharedPreferences.edit().putString("prefRightDirectionButtons", "").commit();
                        sharedPreferences.edit().putString("prefLeftDirectionButtonsShort", "").commit();
                        sharedPreferences.edit().putString("prefRightDirectionButtonsShort", "").commit();
                        parentActivity.forceReLaunchAppOnPreferencesClose = true;
                        parentActivity.forceRestartApp(mainapp.FORCED_RESTART_REASON_LOCALE);
                        break;

                    case "prefThrottleScreenType":
                        parentActivity.prefThrottleScreenType = prefs.getString("prefThrottleScreenType", parentActivity.getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
                        parentActivity.checkThrottleScreenType(sharedPreferences);
                        parentActivity.showHideThrottleSwitchPreferences(getPreferenceScreen());
                        showHideThrottleWebViewPreferences(sharedPreferences);
                        if ( (parentActivity.prefThrottleScreenType.equals("Simple"))
                           && (prefs.getString("prefSimpleThrottleLayoutShowFunctionButtonCount", parentActivity.getApplicationContext().getResources().getString(R.string.prefSimpleThrottleLayoutShowFunctionButtonCountDefaultValue)).equals("0"))
                           && ( (!prefs.getString("prefDeviceSounds0", parentActivity.getApplicationContext().getResources().getString(R.string.prefDeviceSoundsDefaultValue)).equals("none"))
                              || (!prefs.getString("prefDeviceSounds0", parentActivity.getApplicationContext().getResources().getString(R.string.prefDeviceSoundsDefaultValue)).equals("none")) ) ) {
                            Toast.makeText(context, R.string.toastDeviceSoundsSimpleLayoutWarning, Toast.LENGTH_LONG).show();
                        }
                    case "NumThrottle":
                        showHideThrottleNumberPreference(sharedPreferences);
                        if (!parentActivity.ignoreThisThrottleNumChange) {
                            parentActivity.limitNumThrottles(getPreferenceScreen(), sharedPreferences);
                        }
                        parentActivity.ignoreThisThrottleNumChange = false;
                        break;

                    case "prefTheme":
                        String prefTheme = sharedPreferences.getString("prefTheme", parentActivity.getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
                        if (!prefTheme.equals(prefThemeOriginal)) {
                            parentActivity.forceRestartApp(mainapp.FORCED_RESTART_REASON_THEME);
                        }
                        break;

                    case "prefShowAdvancedPreferences":
                        parentActivity.reload();
                        break;

                    case "prefAllowMobileData":
                        parentActivity.mainapp.haveForcedWiFiConnection = false;
                        parentActivity.forceRestartAppOnPreferencesCloseReason = mainapp.FORCED_RESTART_REASON_FORCE_WIFI;
                        parentActivity.forceReLaunchAppOnPreferencesClose = true;
                        break;

                    case "prefFeedbackOnDisconnect":
                        mainapp.prefFeedbackOnDisconnect = sharedPreferences.getBoolean("prefFeedbackOnDisconnect",
                                getResources().getBoolean(R.bool.prefFeedbackOnDisconnectDefaultValue));
                        break;

                    case "prefThrottleViewImmersiveModeHideToolbar":
                        mainapp.prefThrottleViewImmersiveModeHideToolbar = sharedPreferences.getBoolean("prefThrottleViewImmersiveModeHideToolbar",
                                getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeHideToolbarDefaultValue));
                        parentActivity.forceReLaunchAppOnPreferencesClose =true;
                        parentActivity.forceRestartAppOnPreferencesCloseReason =  mainapp.FORCED_RESTART_REASON_IMMERSIVE_MODE;
                        break;

                    case "prefHapticFeedbackButtons":
                        mainapp.prefHapticFeedbackButtons = prefs.getBoolean("prefHapticFeedbackButtons", getResources().getBoolean(R.bool.prefHapticFeedbackButtonsDefaultValue));
                        break;

                }
            }
        }

        void setPreferencesUI() {
            Log.d("Engine_Driver", "Settings: setPreferencesUI()");
            prefs = parentActivity.prefs;
            defaultName = parentActivity.getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);

            mainapp = parentActivity.mainapp;
            if (mainapp != null) {
//                mainapp.applyTheme(parentActivity, true);

                if (!mainapp.isPowerControlAllowed()) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "show_layout_power_button_preference", false);
                }
                if (mainapp.androidVersion < mainapp.minImmersiveModeVersion) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefThrottleViewImmersiveMode", false);
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefThrottleViewImmersiveModeHideToolbar", false);
                }

                if (mainapp.connectedHostName.equals("")) { // option is only available when there is no current connection
                    parentActivity.getConnectionsList();
                    ListPreference preference = (ListPreference) findPreference("prefHostImportExport");
                    if (preference!=null) {
                        preference.setEntries(parentActivity.prefHostImportExportEntriesFound);
                        preference.setEntryValues(parentActivity.prefHostImportExportEntryValuesFound);
                    }
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefAllowMobileData", true);
                } else {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefAllowMobileData", false);
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefHostImportExport", false);
                }

                if (mainapp.androidVersion < mainapp.minActivatedButtonsVersion) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefSelectedLocoIndicator", false);
                }

                if ((mainapp.connectedHostip == null) || (mainapp.web_server_port == 0)) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(),  "prefImportServerManual", false);
                }

                iplsLoader.getIplsList();
                int ipslCount = mainapp.iplsNames.size();
                int deviceSoundsCount = this.getResources().getStringArray(R.array.deviceSoundsEntries).length;
                deviceSoundsEntriesArray = new String[deviceSoundsCount + ipslCount];
                deviceSoundsEntryValuesArray = new String[deviceSoundsCount + ipslCount];
                for (int i=0; i<deviceSoundsCount; i++) {
                    deviceSoundsEntriesArray[i] = this.getResources().getStringArray(R.array.deviceSoundsEntries)[i];
                    deviceSoundsEntryValuesArray[i] = this.getResources().getStringArray(R.array.deviceSoundsEntryValues)[i];
                }
                if (!mainapp.iplsNames.isEmpty()) {
                    for (int i=0; i<ipslCount; i++) {
                        deviceSoundsEntriesArray[deviceSoundsCount+i] = mainapp.iplsNames.get(i);
                        deviceSoundsEntryValuesArray[deviceSoundsCount+i] = mainapp.iplsFileNames.get(i);
                    }
                }

                ListPreference lp = (ListPreference) findPreference("prefDeviceSounds0");
                lp.setEntries(deviceSoundsEntriesArray);
                lp.setEntryValues(deviceSoundsEntryValuesArray);

                lp = (ListPreference) findPreference("prefDeviceSounds1");
                lp.setEntries(deviceSoundsEntriesArray);
                lp.setEntryValues(deviceSoundsEntryValuesArray);
            }


            if (prefs != null) {
                prefThemeOriginal = prefs.getString("prefTheme",
                        parentActivity.getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
                if (mainapp.androidVersion < mainapp.minThemeVersion) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefTheme", false);
                }

                parentActivity.result = RESULT_OK;

                // Disable ESU MCII preferences if not an ESU MCII
                if (!MobileControl2.isMobileControl2()) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefEsuMc2", false);
                }

                parentActivity.enableDisablePreference(getPreferenceScreen(), "prefFlashlightButtonDisplay", mainapp.isFlashlightAvailable());

                parentActivity.deviceId = Settings.System.getString(parentActivity.getContentResolver(), Settings.Secure.ANDROID_ID);
                prefs.edit().putString("prefAndroidId", parentActivity.deviceId).commit();

                // - - - - - - - - - - - -

                parentActivity.prefThrottleScreenType = prefs.getString("prefThrottleScreenType", parentActivity.getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
                parentActivity.prefThrottleScreenTypeOriginal = parentActivity.prefThrottleScreenType;

                showHideThrottleTypePreferences();
                showHideThrottleNumberPreference(prefs);
                showHideThrottleWebViewPreferences(prefs);

                prefs.edit().putBoolean("prefForcedRestart", false).commit();
                prefs.edit().putInt("prefForcedRestartReason", mainapp.FORCED_RESTART_REASON_NONE).commit();
                prefs.edit().putString("prefPreferencesImportAll", PREF_IMPORT_ALL_RESET).commit();

                if (!prefs.getString("prefImportExport", parentActivity.getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue)).equals("None")) {
                    // preference is still confused after a reload or reset
                    prefs.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
                }

                parentActivity.prefConsistFollowRuleStyle = prefs.getString("prefConsistFollowRuleStyle", parentActivity.getApplicationContext().getResources().getString(R.string.prefConsistFollowRuleStyleDefaultValue));

                parentActivity.prefThrottleSwitchButtonDisplay = prefs.getBoolean("prefThrottleSwitchButtonDisplay", false);

                setSwipeThroughWebPreference();

                advancedPreferences = getResources().getStringArray(R.array.advancedPreferences);
                hideAdvancedPreferences();
            }

        }

        @SuppressLint("ApplySharedPref")
        static void putObject(SharedPreferences sharedPreferences, final String key, final Object val) {
            if (val instanceof Boolean)
                sharedPreferences.edit().putBoolean(key, (Boolean) val).commit();
            else if (val instanceof Float)
                sharedPreferences.edit().putFloat(key, (Float) val).commit();
            else if (val instanceof Integer)
                sharedPreferences.edit().putInt(key, (Integer) val).commit();
            else if (val instanceof Long)
                sharedPreferences.edit().putLong(key, (Long) val).commit();
            else if (val instanceof String)
                sharedPreferences.edit().putString(key, ((String) val)).commit();
        }

        private void showHideThrottleNumberPreference(SharedPreferences sharedPreferences) {
            Log.d("Engine_Driver", "Settings: showHideThrottleNumberPreference()");
            boolean enable = true;
            parentActivity.prefThrottleScreenType = prefs.getString("prefThrottleScreenType", parentActivity.getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
            int index = parentActivity.getThrottleScreenTypeArrayIndex(sharedPreferences);
            int[] fixed = this.getResources().getIntArray(R.array.prefThrottleScreenTypeFixedThrottleNumber);

            if ((index >=0) && (index<fixed.length) && (fixed[index] == 0)) {
                enable = false;
            }

            parentActivity.enableDisablePreference(getPreferenceScreen(), "NumThrottle", !enable);
        }

        private void showHideThrottleWebViewPreferences(SharedPreferences sharedPreferences) {
            Log.d("Engine_Driver", "Settings: showHideThrottleWebViewPreferences()");
            boolean enable = parentActivity.throttleScreenTypeSupportsWebView(sharedPreferences);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "throttle_webview_preference", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefWebViewButton", enable);
        }

        private void showHideSimpleThrottleLayoutShowFunctionButtonCountPreference() {
            boolean enable = true;
            if (parentActivity.prefThrottleSwitchButtonDisplay) {
                enable = false;
            }
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefSimpleThrottleLayoutShowFunctionButtonCount", !enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefThrottleSwitchOption2", !enable);
        }

        private void setSwipeThroughWebPreference() {
            String to = prefs.getString("ThrottleOrientation",
                    getResources().getString(R.string.prefThrottleOrientationDefaultValue));
            if (to.equals("Auto-Web")) {
                parentActivity.enableDisablePreference(getPreferenceScreen(), "swipe_through_web_preference", false);
                boolean swipeWeb = prefs.getBoolean("swipe_through_web_preference",
                        getResources().getBoolean(R.bool.prefSwipeThroughWebDefaultValue));
                if (swipeWeb) {
                    prefs.edit().putBoolean("swipe_through_web_preference", false).commit();  //make sure preference is off
                    Toast.makeText(parentActivity.getApplicationContext(), parentActivity.getApplicationContext().getResources()
                            .getString(R.string.toastPreferencesSwipeThroughWebDisabled), Toast.LENGTH_LONG).show();
                }
            } else {
                parentActivity.enableDisablePreference(getPreferenceScreen(), "swipe_through_web_preference", true);
            }
        }


        private void showHideThrottleTypePreferences() {
            Log.d("Engine_Driver", "Settings: showHideThrottleTypePreferences()");
            boolean enable = true;
            if ((parentActivity.prefThrottleScreenType.equals("Simple")) || (parentActivity.prefThrottleScreenType.equals("Vertical"))
                    || (parentActivity.prefThrottleScreenType.equals("Vertical Left"))  || (parentActivity.prefThrottleScreenType.equals("Vertical Right"))
                    || (parentActivity.prefThrottleScreenType.equals("Switching"))
                    || (parentActivity.prefThrottleScreenType.equals("Tablet Switching Left"))
                    || (parentActivity.prefThrottleScreenType.equals("Tablet Vertical Left"))
                    || (parentActivity.prefThrottleScreenType.equals("Switching Left"))  || (parentActivity.prefThrottleScreenType.equals("Switching Right"))) {
                enable = false;
            }
            parentActivity.enableDisablePreference(getPreferenceScreen(), "increase_slider_height_preference", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "left_slider_margin", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefHideSliderAndSpeedButtons", enable);

            enable = !parentActivity.prefThrottleScreenType.equals("Simple");
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefAlwaysUseDefaultFunctionLabels", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefNumberOfDefaultFunctionLabels", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefNumberOfDefaultFunctionLabelsForRoster", enable);

            enable = parentActivity.prefThrottleScreenType.equals("Default")
                    || parentActivity.prefThrottleScreenType.equals("Vertical")
                    || parentActivity.prefThrottleScreenType.equals("Vertical Left")
                    || parentActivity.prefThrottleScreenType.equals("Vertical Right")
                    || parentActivity.prefThrottleScreenType.equals("Switching")
                    || parentActivity.prefThrottleScreenType.equals("Switching Left")
                    || parentActivity.prefThrottleScreenType.equals("Switching Right")
                    || parentActivity.prefThrottleScreenType.equals("Switching Horizontal")
                    || parentActivity.prefThrottleScreenType.equals("Tablet Switching Left")
                    || parentActivity.prefThrottleScreenType.equals("Tablet Vertical Left")
                    || parentActivity.prefThrottleScreenType.equals("Simple");
            parentActivity.enableDisablePreference(getPreferenceScreen(), "WebViewLocation", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefIncreaseWebViewSize", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "InitialThrotWebPage", enable);

            enable = !parentActivity.prefThrottleScreenType.equals("Default");
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefTickMarksOnSliders", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefVerticalStopButtonMargin", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefLimitSpeedButton", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefLimitSpeedPercent", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefPauseSpeedButton", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefPauseSpeedRate", enable);
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefPauseSpeedStep", enable);

            enable = parentActivity.prefThrottleScreenType.equals("Default");
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefDecreaseLocoNumberHeight", enable);

            enable = parentActivity.prefThrottleScreenType.equals("Simple");
            parentActivity.enableDisablePreference(getPreferenceScreen(), "prefSimpleThrottleLayoutShowFunctionButtonCount", enable);
        }


        public void removePreference(Preference preference) {
            try {
                PreferenceGroup parent = getParent(getPreferenceScreen(), preference);
                if (parent != null)
                    parent.removePreference(preference);
                else //Doesn't have a parent
                    getPreferenceScreen().removePreference(preference);
            } catch (Exception except) {
                Log.d("Engine_Driver", "Settings: removePreference: failed: " + preference);
                return;
            }
        }

        private void hideAdvancedPreferences() {
            if (!prefs.getBoolean("prefShowAdvancedPreferences", parentActivity.getApplicationContext().getResources().getBoolean(R.bool.prefShowAdvancedPreferencesDefaultValue) ) ) {
                for (String advancedPreference1 : advancedPreferences) {
// //                Log.d("Engine_Driver", "Settings: hideAdvancedPreferences(): " + advancedPreference1);
                    Preference advancedPreference = (Preference) findPreference(advancedPreference1);
                    if (advancedPreference != null) {
                        removePreference(advancedPreference);
                    } else {
                        Log.d("Engine_Driver", "Settings: '" + advancedPreference1 + "' not found.");
                    }
                }
            }
        }

        private PreferenceGroup getParent(PreferenceGroup groupToSearchIn, Preference preference) {
            for (int i = 0; i < groupToSearchIn.getPreferenceCount(); ++i) {
                Preference child = groupToSearchIn.getPreference(i);

                if (child == preference)
                    return groupToSearchIn;

                if (child instanceof PreferenceGroup) {
                    PreferenceGroup childGroup = (PreferenceGroup)child;
                    PreferenceGroup result = getParent(childGroup, preference);
                    if (result != null)
                        return result;
                }
            }

            return null;
        }

    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public static class SettingsSubScreenFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String TAG = SettingsSubScreenFragment.class.getName();
        public static final String PAGE_ID = "page_id";
        SettingsActivity parentActivity;
        public String[] advancedSubPreferences;

        public static SettingsSubScreenFragment newInstance(String pageId) {
            SettingsSubScreenFragment f = new SettingsSubScreenFragment();
            Bundle args = new Bundle();
            args.putString(PAGE_ID, pageId);
            f.setArguments(args);

            return (f);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            // rootKey is the name of preference sub screen key name , here--customPrefKey
            setPreferencesFromResource(R.xml.preferences, rootKey);
            Log.d(TAG, "onCreatePreferences of the sub screen " + rootKey);

            Activity a = getActivity();
            parentActivity = (SettingsActivity) a;
            if (parentActivity.prefs==null) {
                parentActivity.loadSharedPreferences();
            }

            parentActivity.isInSubScreen = true;

            parentActivity.setGamePadPrefLabels(getPreferenceScreen(), parentActivity.prefs);
            parentActivity.prefs.edit().putBoolean("prefGamepadTestNow", false).commit();  //reset the preference

            parentActivity.prefBackgroundImage = parentActivity.prefs.getBoolean("prefBackgroundImage", false);
            parentActivity.showHideBackgroundImagePreferences(getPreferenceScreen());

            parentActivity.showHideWebSwipePreferences(getPreferenceScreen());

            parentActivity.showHideTTSPreferences(getPreferenceScreen());

            parentActivity.showHideConsistRuleStylePreferences(getPreferenceScreen());

            parentActivity.showHideThrottleSwitchPreferences(getPreferenceScreen());

            // option is only available when there is no current connection

            if (parentActivity.mainapp != null) {
                if (parentActivity.mainapp.connectedHostName.equals("")) {
                    parentActivity.getConnectionsList();
                    ListPreference preference = (ListPreference) findPreference("prefHostImportExport");
                    if (preference != null) {
                        preference.setEntries(parentActivity.prefHostImportExportEntriesFound);
                        preference.setEntryValues(parentActivity.prefHostImportExportEntryValuesFound);
                    }
                } else {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefHostImportExport", false);
                }
            }

            advancedSubPreferences = getResources().getStringArray(R.array.advancedSubPreferences);
            hideAdvancedSubPreferences();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Set the default white background in the view so as to avoid transparency
//            view.setBackgroundColor(
//                    ContextCompat.getColor(getContext(), R.color.background_material_light));

        }

        @Override
        public void onResume() {
            Log.d("Engine_Driver", "Settings: SettingsFragment onResume()");
            super.onResume();

            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals("prefBackgroundImageFileNameImagePicker")) {
                parentActivity.loadImagefromGallery();
                return true;
            } else {
                super.onPreferenceTreeClick(preference);
            }
            return false;
        }

        private PreferenceGroup getParent(PreferenceGroup groupToSearchIn, Preference preference) {
            for (int i = 0; i < groupToSearchIn.getPreferenceCount(); ++i) {
                Preference child = groupToSearchIn.getPreference(i);

                if (child == preference)
                    return groupToSearchIn;

                if (child instanceof PreferenceGroup) {
                    PreferenceGroup childGroup = (PreferenceGroup)child;
                    PreferenceGroup result = getParent(childGroup, preference);
                    if (result != null)
                        return result;
                }
            }
            return null;
        }

        public void removeSubPreference(Preference preference) {
            try {
                PreferenceGroup parent = getParent(getPreferenceScreen(), preference);
                if (parent != null)
                    parent.removePreference(preference);
                else //Doesn't have a parent
                    getPreferenceScreen().removePreference(preference);
            } catch (Exception except) {
                Log.d("Engine_Driver", "Settings: removeSubPreference: failed: " + preference);
                return;
            }
        }

        private void hideAdvancedSubPreferences() {
            if (!parentActivity.prefs.getBoolean("prefShowAdvancedPreferences", parentActivity.getApplicationContext().getResources().getBoolean(R.bool.prefShowAdvancedPreferencesDefaultValue) ) ) {
                for (String advancedSubPreference1 : advancedSubPreferences) {
// //                Log.d("Engine_Driver", "Settings: hideAdvancedPreferences(): " + advancedPreference1);
                    Preference advancedSubPreference = (Preference) findPreference(advancedSubPreference1);
                    if (advancedSubPreference != null) {
                        removeSubPreference(advancedSubPreference);
                    } else {
                        Log.d("Engine_Driver", "Settings: '" + advancedSubPreference1 + "' not found.");
                    }
                }
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) {
            boolean prefForcedRestart = sharedPreferences.getBoolean("prefForcedRestart", false);

            if ((key != null) && !prefForcedRestart) {  // don't do anything if the preference have been loaded and we are about to reload the app.
                switch (key) {
                    case "prefImportExport":
                        if (!parentActivity.importExportPreferences.currentlyImporting) {
                            parentActivity.exportedPreferencesFileName = "exported_preferences.ed";
                            String currentValue = sharedPreferences.getString(key, "");
                            if (currentValue.equals(IMPORT_EXPORT_OPTION_EXPORT)) {
                                parentActivity.saveSharedPreferencesToFile(sharedPreferences,
                                        parentActivity.exportedPreferencesFileName, true);
                            } else if (currentValue.equals(IMPORT_EXPORT_OPTION_IMPORT)) {
                                parentActivity.loadSharedPreferencesFromFileDialog(sharedPreferences,
                                        parentActivity.exportedPreferencesFileName, parentActivity.deviceId,
                                        parentActivity.mainapp.FORCED_RESTART_REASON_IMPORT);
                            } else if (currentValue.equals(IMPORT_EXPORT_OPTION_RESET)) {
                                parentActivity.resetPreferencesDialog();
                            }
                        }
                        break;
                    case "prefPreferencesImportAll":
//                        String val = sharedPreferences.getString("prefImportServerManual", parentActivity.getApplicationContext().getResources().getString(R.string.prefImportServerManualDefaultValue));
//                        val = val.trim();
//                        if (!(sharedPreferences.getString("prefPreferencesImportAll", "").equals(PREF_IMPORT_ALL_RESET))) {
//                            new preferences.importFromURL().execute(val);
//                        }
                        break;
                    case "prefHostImportExport":
                        if (!parentActivity.importExportPreferences.currentlyImporting) {
                            String currentValue = sharedPreferences.getString(key, "");
                            if (!currentValue.equals(IMPORT_EXPORT_OPTION_NONE)) {
                                String action = currentValue.substring(0, IMPORT_PREFIX.length());
                                parentActivity.exportedPreferencesFileName = currentValue.substring(IMPORT_PREFIX.length(), currentValue.length());
                                if (action.equals(EXPORT_PREFIX)) {
                                    parentActivity.saveSharedPreferencesToFile(sharedPreferences,
                                            parentActivity.exportedPreferencesFileName, true);
                                } else if (action.equals(IMPORT_PREFIX)) {
                                    parentActivity.loadSharedPreferencesFromFile(sharedPreferences,
                                            parentActivity.exportedPreferencesFileName, parentActivity.deviceId,
                                            parentActivity.mainapp.FORCED_RESTART_REASON_IMPORT);
                                }
                            }
                        }
                        break;
                    case "prefGamepadTestNow":
                        parentActivity.start_gamepad_test_activity();
                        break;
                    case "prefGamePadFeedbackVolume":
                        //limit check new value
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key,
                                ToneGenerator.MIN_VOLUME, ToneGenerator.MAX_VOLUME,
                                parentActivity.getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue));
                        parentActivity.result = RESULT_GAMEPAD;
                        break;
                    case "prefGamePadType":
                        parentActivity.setGamePadPrefLabels(getPreferenceScreen(), sharedPreferences);
                    case "prefGamePadStartButton":
                        parentActivity.result = RESULT_GAMEPAD;
                        break;

                    case "prefBackgroundImage":
                        parentActivity.prefBackgroundImage = sharedPreferences.getBoolean("prefBackgroundImage", false);
                        parentActivity.showHideBackgroundImagePreferences(getPreferenceScreen());
                    case "prefBackgroundImageFileName":
                    case "prefBackgroundImagePosition":
                        parentActivity.forceRestartAppOnPreferencesClose = true;
                        parentActivity.forceRestartAppOnPreferencesCloseReason = parentActivity.mainapp.FORCED_RESTART_REASON_BACKGROUND;
                        break;

                    case "prefTtsWhen":
                        parentActivity.showHideTTSPreferences(getPreferenceScreen());
                        break;

                    case "prefConsistFollowRuleStyle":
                        parentActivity.prefConsistFollowRuleStyle = sharedPreferences.getString("prefConsistFollowRuleStyle", parentActivity.getApplicationContext().getResources().getString(R.string.prefConsistFollowRuleStyleDefaultValue));
                        parentActivity.showHideConsistRuleStylePreferences(getPreferenceScreen());
                        break;

                    case "prefThrottleSwitchButtonDisplay":
                        parentActivity.prefThrottleSwitchButtonDisplay
                                = sharedPreferences.getBoolean("prefThrottleSwitchButtonDisplay", false);
                        parentActivity.showHideThrottleSwitchPreferences(getPreferenceScreen());
                        break;

                    case "prefAccelerometerShakeThreshold":
                        parentActivity.limitFloatPrefValue(getPreferenceScreen(), sharedPreferences, key, 1.2F, 3.0F, "2.0"); // limit check new value
                        parentActivity.forceRestartAppOnPreferencesCloseReason = parentActivity.mainapp.FORCED_RESTART_REASON_SHAKE_THRESHOLD;
                        parentActivity.forceRestartAppOnPreferencesClose = true;
                        break;

                    case "hide_slider_preference":
                        parentActivity.prefHideSlider = sharedPreferences.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue));
                        break;

                    case "WebViewLocation":
                        parentActivity.mainapp.alert_activities(message_type.WEBVIEW_LOC, "");
                        break;
                    case "InitialThrotWebPage":
                        parentActivity.mainapp.alert_activities(message_type.INITIAL_THR_WEBPAGE, "");
                        break;

                    case "prefDirectionButtonLongPressDelay":
                        // limit check new value
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 500, 9999, "1000");
                        break;

                    case "prefEsuMc2ZeroTrim":
                        // limit check new value
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 0, 255, "10");
                        parentActivity.result = RESULT_ESUMCII;
                        break;
                    case "prefEsuMc2ButtonsRepeatDelay":
                        // limit check new value
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 0, 9999, "500");
                        break;
                    case "prefEsuMc2StopButtonDelay":
                        // limit check new value
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 0, 9999, "500");
                        break;

                    case "prefShowTimeOnLogEntry":
                        parentActivity.mainapp.prefShowTimeOnLogEntry = sharedPreferences.getBoolean("prefShowTimeOnLogEntry",
                                getResources().getBoolean(R.bool.prefShowTimeOnLogEntryDefaultValue));
                        break;

                    case "prefSwitchingThrottleSliderDeadZone":
                        parentActivity.forceRestartAppOnPreferencesCloseReason = parentActivity.mainapp.FORCED_RESTART_REASON_DEAD_ZONE;
                        parentActivity.forceRestartAppOnPreferencesClose = true;
                        break;

                }
            }
        }
    }
}