/* Copyright (C) 2017-2026 M. Steve Todd mstevetodd@gmail.com

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

 * This helper class for importing and exporting shared preferences to SD card files.
 * Created by Peter Akers on 17-Dec-17.
*/

package jmri.enginedriver.import_export;

import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.Loco;
import jmri.enginedriver.type.address_type;
import jmri.enginedriver.type.light_follow_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.restart_reason_type;
import jmri.enginedriver.type.source_type;
import jmri.enginedriver.type.pref_import_type;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

/** @noinspection CallToPrintStackTrace*/
public class ImportExportPreferences {
    static final String activityName = "ImportExportPreferences";

    public boolean currentlyImporting = false;

    public static final String RECENT_TURNOUTS_FILENAME = "recent_turnouts_list.txt";
    private static final String RECENT_CONSISTS_FILENAME = "recent_consist_list.txt";
    private static final String RECENT_ENGINES_FILENAME = "recent_engine_list.txt";
    private static final String THROTTLES_ENGINES_FILENAME = "throttles_engines_list.txt";

    public ArrayList<Integer> recentLocoAddressList;
    public ArrayList<Integer> recentLocoAddressSizeList; // Look at address_type.java
    public ArrayList<String> recentLocoNameList;
//    ArrayList<String> recentLocoNameHtmlList;
    public ArrayList<Integer> recentLocoSourceList;
    public ArrayList<String> recentLocoFunctionsList;

    public ArrayList<ArrayList<Integer>> recentConsistLocoAddressList = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> recentConsistAddressSizeList = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> recentConsistDirectionList = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> recentConsistSourceList = new ArrayList<>();
    public ArrayList<ArrayList<String>> recentConsistRosterNameList = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> recentConsistLightList = new ArrayList<>();  // placeholder - not currently use
    public ArrayList<String> recentConsistNameList = new ArrayList<>();
    public ArrayList<String> recentConsistNameHtmlList = new ArrayList<>();

    public ArrayList<String> recentTurnoutAddressList;
    public ArrayList<String> recentTurnoutNameList;
    public ArrayList<Integer> recentTurnoutSourceList;
    public ArrayList<String> recentTurnoutServerList;

    private void writeExportFile(threaded_application mainapp, Context context, SharedPreferences sharedPreferences, String exportedPreferencesFileName){
        Log.d(threaded_application.applicationName, activityName + ": writeExportFile(): Writing export file");
        boolean result = false;
        ObjectOutputStream output = null;

        File dst = new File(context.getExternalFilesDir(null), exportedPreferencesFileName);

        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            output.writeObject(sharedPreferences.getAll());
            @SuppressLint("StringFormatMatches") String m = context.getResources().getString(R.string.toastImportExportExportSucceeded,exportedPreferencesFileName);
            mainapp.safeToast(m, Toast.LENGTH_SHORT);
            Log.d(threaded_application.applicationName, activityName + ": " + m);
            result = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (!result) {
            Log.e(threaded_application.applicationName, activityName + ": writeExportFile(): Export Failed");
            mainapp.safeToast(R.string.toastImportExportExportFailed, Toast.LENGTH_LONG);
        } else {
            Log.d(threaded_application.applicationName, activityName + ": writeExportFile(): Export succeeded");

        }
    }

    public void writeSharedPreferencesToFile(threaded_application mainapp, Context context, SharedPreferences sharedPreferences, String exportedPreferencesFileName) {
        Log.d(threaded_application.applicationName, activityName + ": writeSharedPreferencesToFile(): Saving preferences to file");

        boolean prefImportExportLocoList = sharedPreferences.getBoolean("prefImportExportLocoList", context.getResources().getBoolean(R.bool.prefImportExportLocoListDefaultValue));
        if (prefImportExportLocoList) {
            recentLocoAddressList = new ArrayList<>();
            recentLocoAddressSizeList = new ArrayList<>();
            recentLocoNameList = new ArrayList<>();
            recentLocoSourceList = new ArrayList<>();
            recentLocoFunctionsList = new ArrayList<>();
            loadRecentLocosListFromFile(context);
            saveIntListDataToPreferences(recentLocoAddressList, "prefRecentLoco", sharedPreferences);
            saveIntListDataToPreferences(recentLocoAddressSizeList, "prefRecentLocoSize", sharedPreferences);
            saveStringListDataToPreferences(recentLocoNameList, "prefRecentLocoName", sharedPreferences);
            saveIntListDataToPreferences(recentLocoSourceList, "prefRecentLocoSource", sharedPreferences);
            saveStringListDataToPreferences(recentLocoFunctionsList, "prefRecentLocoFunction", sharedPreferences);

            loadRecentConsistsListFromFile(context);
            saveStringListDataToPreferences(recentConsistNameList, "prefRecentConsistName", sharedPreferences);
            // note recentConsistNameHtmlList is not save or loaded. it is generated as needed
            for (int i = 0; i < recentConsistNameList.size(); i++) {
                saveIntListDataToPreferences(recentConsistLocoAddressList.get(i), "prefRecentConsistAddress_"+i, sharedPreferences);
                saveIntListDataToPreferences(recentConsistAddressSizeList.get(i), "prefRecentConsistSize_"+i, sharedPreferences);
                saveIntListDataToPreferences(recentConsistDirectionList.get(i), "prefRecentConsistDirection_"+i, sharedPreferences);
                saveIntListDataToPreferences(recentConsistSourceList.get(i), "prefRecentConsistSource_"+i, sharedPreferences);
                saveStringListDataToPreferences(recentConsistRosterNameList.get(i), "prefRecentConsistRosterName_"+i, sharedPreferences);
                saveIntListDataToPreferences(recentConsistLightList.get(i), "prefRecentConsistLight_"+i, sharedPreferences);
            }
        }

        if (!exportedPreferencesFileName.equals(".ed")) {
            writeExportFile(mainapp, context, sharedPreferences, exportedPreferencesFileName);
        } else {
            mainapp.safeToast(R.string.toastImportExportExportFailed, Toast.LENGTH_LONG);
        }

        int numberOfRecentLocosToWrite = getIntPrefValue(sharedPreferences, "prefMaximumRecentLocos", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue));

        int prefCount;
        if (prefImportExportLocoList) {  // now clean out the preference data

            Log.d(threaded_application.applicationName, activityName + ": writeSharedPreferencesToFile():  Normal Cleanout of old Recent Locos preferences");
            prefCount = removeExtraListDataFromPreferences(0,numberOfRecentLocosToWrite+1,"prefRecentLoco", sharedPreferences);
            if (prefCount == numberOfRecentLocosToWrite+1) {  // if there were that many, assume the worst
                Log.d(threaded_application.applicationName, activityName + ": writeSharedPreferencesToFile():  Extended Cleanout of old Recent Locos preferences");
                prefCount = removeExtraListDataFromPreferences(numberOfRecentLocosToWrite+1, 600, "prefRecentLoco", sharedPreferences);
            }
            removeExtraListDataFromPreferences(0, prefCount,"prefRecentLocoSize", sharedPreferences);
            removeExtraListDataFromPreferences(0,prefCount,"prefRecentLocoName", sharedPreferences);
            removeExtraListDataFromPreferences(0,prefCount,"prefRecentLocoSource", sharedPreferences);

            prefCount =removeExtraListDataFromPreferences(0,numberOfRecentLocosToWrite+1,"prefRecentConsistName", sharedPreferences);
            if (prefCount == numberOfRecentLocosToWrite+1) {  // if there were that many, look for more
                Log.d(threaded_application.applicationName, activityName + ": writeSharedPreferencesToFile():  Extended Cleanout of old Recent Locos preferences");
                prefCount = removeExtraListDataFromPreferences(numberOfRecentLocosToWrite+1, numberOfRecentLocosToWrite+20, "prefRecentConsistName", sharedPreferences);

                if (prefCount == numberOfRecentLocosToWrite+20) {  // if there were that many, assume the worst
                    Log.d(threaded_application.applicationName, activityName + ": writeSharedPreferencesToFile():  Extended DEEP CLEAN of old Recent Locos preferences");
                    prefCount = removeExtraListDataFromPreferences(numberOfRecentLocosToWrite+1, 600, "prefRecentConsistName", sharedPreferences);
                }
            }

            Log.d(threaded_application.applicationName, activityName + ": writeSharedPreferencesToFile():  Normal Cleanout of old Recent Consist preferences");
            for (int i = 0; i < numberOfRecentLocosToWrite; i++) {
                int subPrefCount = removeExtraListDataFromPreferences(0,10,"prefRecentConsistAddress_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistSize_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistDirection_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistSource_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistRosterName_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistLight_"+i, sharedPreferences);
            }

            if (sharedPreferences.contains("prefRecentTurnoutServer_0")) {  // there should not be any so assume the worst
                Log.d(threaded_application.applicationName, activityName + ": writeSharedPreferencesToFile():  Extended Cleanout of old Recent turnouts preferences - these should not exist");
                prefCount = removeExtraListDataFromPreferences(0, 600, "prefRecentTurnout", sharedPreferences);
                removeExtraListDataFromPreferences(0, prefCount, "prefRecentTurnoutName", sharedPreferences);
                removeExtraListDataFromPreferences(0, prefCount, "prefRecentTurnoutSource", sharedPreferences);
                removeExtraListDataFromPreferences(0, prefCount, "prefRecentTurnoutServer", sharedPreferences);
            }
        }
        Log.d(threaded_application.applicationName, activityName + ": writeSharedPreferencesToFile(): ImportExportPreferences: Saving preferences to file - Finished");
    }

    @SuppressLint({"ApplySharedPref", "StringFormatMatches"})
    public boolean loadSharedPreferencesFromFile(threaded_application mainapp, Context context, SharedPreferences sharedPreferences, String exportedPreferencesFileName, String deviceId, boolean clearRecentsIfNoFile) {
        Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Loading saved preferences from file");
        currentlyImporting = true;
        boolean res = false;
        boolean srcExists = false;
        SharedPreferences.Editor prefEdit = sharedPreferences.edit();

        if (!exportedPreferencesFileName.equals(".ed")) {

            // save a few values so that we can reset them. i.e. effectively don't import them
            String currentThrottleNameValue = sharedPreferences.getString("prefThrottleName", context.getResources().getString(R.string.prefThrottleNameDefaultValue)).trim();
            String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", context.getResources().getString(R.string.prefAutoImportExportDefaultValue));
//            boolean prefForcedRestart = sharedPreferences.getBoolean("prefForcedRestart", false);
            int prefForcedRestartReason = sharedPreferences.getInt("prefForcedRestartReason", restart_reason_type.NONE);
            boolean prefImportExportLocoList = sharedPreferences.getBoolean("prefImportExportLocoList", context.getResources().getBoolean(R.bool.prefImportExportLocoListDefaultValue));
            String prefPreferencesImportFileName = sharedPreferences.getString("prefPreferencesImportFileName", "");
            String prefAndroidId = sharedPreferences.getString("prefAndroidId", "");

            String prefPreferencesImportAll = sharedPreferences.getString("prefPreferencesImportAll", pref_import_type.ALL_FULL);
            String prefTheme = "";
            String prefThrottleScreenType = "Default";
            boolean prefDisplaySpeedButtons = false;
            boolean prefHideSlider = false;

            if (prefPreferencesImportAll.equals(pref_import_type.ALL_PARTIAL)) { // save some additional prefereneces for restoration
                prefTheme = sharedPreferences.getString("prefTheme", "");
                prefThrottleScreenType = sharedPreferences.getString("prefThrottleScreenType", "Default");
                prefDisplaySpeedButtons = sharedPreferences.getBoolean("prefDisplaySpeedButtons", false);
                prefHideSlider = sharedPreferences.getBoolean("prefHideSlider", false);
            }

            File src = new File(context.getExternalFilesDir(null), exportedPreferencesFileName);

            if (src.exists()) {
                srcExists = true;

                ObjectInputStream input = null;
                try {
                    String restoredDeviceId = "";
                    input = new ObjectInputStream(new FileInputStream(src));
                    prefEdit.clear();

                    int i = 0;
                    Map<String, ?> entries = (Map<String, ?>) input.readObject();
                    Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Key Count:" + entries.size());
                    for (Map.Entry<String, ?> entry : entries.entrySet()) {
                        Object v = entry.getValue();
                        String key = entry.getKey();

//                        Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Key Start: " + key);

                        if (v instanceof Boolean) {
//                            Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Key End: " + key + " - boolean - " + v);
                            prefEdit.putBoolean(key, (Boolean) v);
                        } else if (v instanceof Float) {
//                            Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Key End: " + key + " - Float - " + v);
                            prefEdit.putFloat(key, (Float) v);
                        } else if (v instanceof Integer) {
//                            Log.d(threaded_application.applicationName, activityName + ":  loadSharedPreferencesFromFile(): Key End: " + key + " - Integer - " + v);
                            prefEdit.putInt(key, (Integer) v);
                        } else if (v instanceof Long) {
//                            Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Key End: " + key + " - Long - " + v);
                            prefEdit.putLong(key, (Long) v);
                        } else if (v instanceof String) {
//                            Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Key End: " + key + " - String - " + v);
                            prefEdit.putString(key, ((String) v));
                            if (key.equals("prefAndroidId")) { restoredDeviceId = (String) v;}
                        }

                        Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Key " + i +" End: " + key + " - " + v);
                        i++;
                    }
                    res = true;


                    // restore the remembered throttle name to avoid a duplicate throttle name if this is a different to device to where it was originally saved
                    if ((!restoredDeviceId.equals(deviceId)) || (restoredDeviceId.isEmpty())) {
                        prefEdit.putString("prefThrottleName", currentThrottleNameValue);
                    }
                    prefEdit.putString("prefImportExport", "None");  //reset the preference
                    prefEdit.putString("prefHostImportExport", "None");  //reset the preference
                    prefEdit.putString("prefAutoImportExport", prefAutoImportExport);  //reset the preference
                    prefEdit.putBoolean("prefImportExportLocoList", prefImportExportLocoList);  //reset the preference
                    prefEdit.putString("prefRunIntro", threaded_application.INTRO_VERSION);  //don't re-run the intro
                    prefEdit.putBoolean("prefForcedRestart", true);
                    prefEdit.putInt("prefForcedRestartReason", prefForcedRestartReason);
                    prefEdit.putString("prefPreferencesImportFileName", prefPreferencesImportFileName);  //reset the preference
                    prefEdit.putString("prefAndroidId", prefAndroidId);  //reset the preference

                    if (prefPreferencesImportAll.equals(pref_import_type.ALL_PARTIAL)) { // save some additional preferences for restoration
                        prefEdit.putString("prefTheme", prefTheme);
                        prefEdit.putString("prefThrottleScreenType", prefThrottleScreenType);
                        prefEdit.putBoolean("prefDisplaySpeedButtons", prefDisplaySpeedButtons);
                        prefEdit.putBoolean("prefHideSlider", prefHideSlider);
                    }
                    prefEdit.putString("prefPreferencesImportAll", pref_import_type.ALL_RESET); // reset the preference

                    @SuppressLint("StringFormatMatches") String m = context.getResources().getString(R.string.toastImportExportImportSucceeded, exportedPreferencesFileName);

                    Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): " + m);
                    mainapp.safeToast(m, Toast.LENGTH_SHORT);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Exception: " + e);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Exception: " + e);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    Log.e(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Exception: " + e);
                } finally {
                    try {
                        if (input != null) {
                            input.close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Log.e(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Exception: " + ex);
                    }
                }
                prefEdit.apply();
                prefEdit.commit();
                currentlyImporting = false;

                if (prefImportExportLocoList) {
                    // now take the recent locos list that was stored in the preferences and push them in the file
                    recentLocoAddressList = new ArrayList<>();
                    recentLocoAddressSizeList = new ArrayList<>();
                    recentLocoNameList = new ArrayList<>();
                    recentLocoSourceList = new ArrayList<>();
                    recentLocoFunctionsList = new ArrayList<>();

                    getIntListDataFromPreferences(recentLocoAddressList, "prefRecentLoco", sharedPreferences,-1, 0);
                    getIntListDataFromPreferences(recentLocoAddressSizeList, "prefRecentLocoSize", sharedPreferences, recentLocoAddressList.size(), 0);
                    getStringListDataFromPreferences(recentLocoNameList, "prefRecentLocoName", sharedPreferences, recentLocoAddressList.size(), "");
                    getIntListDataFromPreferences(recentLocoSourceList, "prefRecentLocoSource", sharedPreferences, recentLocoAddressList.size(), source_type.UNKNOWN);
                    getStringListDataFromPreferences(recentLocoFunctionsList, "prefRecentLocoFunctions", sharedPreferences, recentLocoAddressList.size(), "");
                    writeRecentLocosListToFile(context, sharedPreferences);

                    ArrayList<String> tempRecentConsistNameList = new ArrayList<>();
                    getStringListDataFromPreferences(tempRecentConsistNameList, "prefRecentConsistName", sharedPreferences, -1, "");
                    // note recentConsistNameHtmlList is not saved or loaded. it is generated as needed
                    for (int i = 0; i < tempRecentConsistNameList.size(); i++) {
                        ArrayList<Integer> tempRecentConsistLocoAddressList_inner = new ArrayList<>();
                        ArrayList<Integer> tempRecentConsistAddressSizeList_inner = new ArrayList<>();
                        ArrayList<Integer> tempRecentConsistDirectionList_inner = new ArrayList<>();
                        ArrayList<Integer> tempRecentConsistSourceList_inner = new ArrayList<>();
                        ArrayList<String> tempRecentConsistRosterNameList_inner = new ArrayList<>();
                        ArrayList<Integer> tempRecentConsistLightList_inner = new ArrayList<>();

                        getIntListDataFromPreferences(tempRecentConsistLocoAddressList_inner, "prefRecentConsistAddress_"+i, sharedPreferences, -1, 0);
                        getIntListDataFromPreferences(tempRecentConsistAddressSizeList_inner, "prefRecentConsistSize_"+i, sharedPreferences, tempRecentConsistLocoAddressList_inner.size(), 0);
                        getIntListDataFromPreferences(tempRecentConsistDirectionList_inner, "prefRecentConsistDirection_"+i, sharedPreferences, tempRecentConsistLocoAddressList_inner.size(), 0);
                        getIntListDataFromPreferences(tempRecentConsistSourceList_inner, "prefRecentConsistSource_"+i, sharedPreferences, tempRecentConsistLocoAddressList_inner.size(), source_type.UNKNOWN);
                        getStringListDataFromPreferences(tempRecentConsistRosterNameList_inner, "prefRecentConsistRosterName_"+i, sharedPreferences, tempRecentConsistLocoAddressList_inner.size(), "");
                        getIntListDataFromPreferences(tempRecentConsistLightList_inner, "prefRecentConsistLight_"+i, sharedPreferences, tempRecentConsistLocoAddressList_inner.size(), light_follow_type.UNKNOWN);

                        addRecentConsistToList(0, tempRecentConsistNameList.get(i),
                                tempRecentConsistLocoAddressList_inner,
                                tempRecentConsistAddressSizeList_inner,
                                tempRecentConsistDirectionList_inner,
                                tempRecentConsistSourceList_inner,
                                tempRecentConsistRosterNameList_inner,
                                tempRecentConsistLightList_inner);

                    }

                    writeRecentConsistsListToFile(context, sharedPreferences, -1);
                }
            } else {
                deleteFile(context, RECENT_ENGINES_FILENAME);
                deleteFile(context, RECENT_CONSISTS_FILENAME);
                deleteFile(context, RECENT_TURNOUTS_FILENAME);
            }

            if (!res) {
                if (srcExists) {
                    mainapp.safeToast(context.getResources().getString(R.string.toastImportExportImportFailed,
                                                        exportedPreferencesFileName), Toast.LENGTH_LONG);
                } else {
                    mainapp.safeToast(context.getResources().getString(R.string.toastImportExportServerImportFailed,
                                                        exportedPreferencesFileName), Toast.LENGTH_LONG);
                }
            }
        } else {
            mainapp.safeToast(R.string.toastImportExportCannotImport, Toast.LENGTH_LONG);
        }

        prefEdit.commit();
        Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFile(): Loading saved preferences from file - Finished");
        return res;
    }

    public void loadRecentLocosListFromFile(Context context) {
        Log.d(threaded_application.applicationName, activityName + ": loadRecentLocosListFromFile()): Loading recent locos list from file");
        if (recentLocoAddressList == null) { //make sure arrays are valid
            recentLocoAddressList = new ArrayList<>();
            recentLocoAddressSizeList = new ArrayList<>();
            recentLocoNameList = new ArrayList<>();
            recentLocoSourceList = new ArrayList<>();
            recentLocoFunctionsList = new ArrayList<>();
        }

        try {
            // Populate the List with the recent engines saved in a file. This
            // will be stored in recent_engine_list.txt
            File engine_list_file = new File(context.getExternalFilesDir(null), RECENT_ENGINES_FILENAME);
            if (engine_list_file.exists()) {
                BufferedReader list_reader = new BufferedReader(
                        new FileReader(engine_list_file));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    int splitPos = line.indexOf(':');
                    if (splitPos > 0) {
                        int addr, size, source = 0;
                        String locoName = "";
                        String functions = "";
                        try {
                            addr = Integer.decode(line.substring(0, splitPos));
                            size = Integer.decode(line.substring(splitPos + 1, splitPos + 2));
                            if (line.length()>splitPos+2) { // has the name extras
                                if (line.charAt(splitPos + 2) == '~') { // old format
                                    locoName = line.substring(splitPos + 3);
                                }else {
                                    if (line.charAt(splitPos + 3) == '~') { // new format. Includes the source
                                        String [] args = threaded_application.splitByString(line.substring(splitPos + 4),"]\\[");
                                        source = Integer.decode(line.substring(splitPos + 2, splitPos + 3));
                                        if (args.length==1) { // only has the name
                                            locoName = args[0];
                                            functions = "";
                                        } else { // has the function map as well
                                            locoName = args[0];
                                            StringBuilder tempFunctions = new StringBuilder();
                                            for (int i = 1; i < args.length; i++) {
                                                tempFunctions.append(args[i]);
                                                if (i < args.length-1) {
                                                    tempFunctions.append("]\\[");
                                                }
                                            }
                                            functions = tempFunctions.toString();
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            addr = -1;
                            size = -1;
                            locoName = "";
                            source = -1;
                            functions = "";
                        }
                        if ((addr >= 0) && (size >= 0)) {
                            String addressLengthString = ((size == 0) ? "S" : "L");  //show L or S based on length from file
                            String engineAddressString = String.format("%s(%s)", addr, addressLengthString);
                            if ((locoName.isEmpty() || locoName.equals(engineAddressString))) { // if nothing is stored, or what is stored is the same as the address, look for it in the roster
                                locoName = engineAddressString;
                            }
                            addRecentLocoToList(addr, size, locoName, source, functions);
                        }
                    }
                }
                list_reader.close();
            }
            Log.d(threaded_application.applicationName, activityName + ": loadRecentLocosListFromFile(): Read recent locos list from file complete successfully");

        } catch (IOException except) {
            Log.e(threaded_application.applicationName, activityName + ": loadRecentLocosListFromFile(): select_loco - Error reading recent loco file. "
                    + except.getMessage());
        }
    }

    public void writeRecentLocosListToFile(Context context, SharedPreferences sharedPreferences) {
        Log.d(threaded_application.applicationName, activityName + ": writeRecentLocosListToFile(): Writing recent locos list to file");

        // write it out from the saved preferences to the file
        File engine_list_file = new File(context.getExternalFilesDir(null), RECENT_ENGINES_FILENAME);

        PrintWriter list_output;
        String smrl = sharedPreferences.getString("prefMaximumRecentLocos", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue)); //retrieve pref for max recent locos to show
        try {
            int mrl = Integer.parseInt(smrl);
            list_output = new PrintWriter(engine_list_file);
            if (mrl > 0) {
                for (int i = 0; i < recentLocoAddressList.size() && i < mrl; i++) {
                    list_output.format("%d:%d%d~%s]\\[%s\n",
                            recentLocoAddressList.get(i),
                            recentLocoAddressSizeList.get(i),
                            recentLocoSourceList.get(i),
                            recentLocoNameList.get(i),
                            recentLocoFunctionsList.get(i));
                }
            }
            list_output.flush();
            list_output.close();
            Log.d(threaded_application.applicationName, activityName + ": writeRecentLocosListToFile(): Write recent locos list to file complete successfully");
        } catch (IOException except) {
            Log.e(threaded_application.applicationName, activityName + ":  writeRecentLocosListToFile(): caught IOException: "
                            + except.getMessage());
        } catch (NumberFormatException except) {
            Log.e(threaded_application.applicationName, activityName + ":  writeRecentLocosListToFile(): caught NumberFormatException: "
                            + except.getMessage());
        } catch (IndexOutOfBoundsException except) {
            Log.e(threaded_application.applicationName, activityName + ":  writeRecentLocosListToFile(): caught IndexOutOfBoundsException: "
                            + except.getMessage());
        }
    }

    public void loadRecentConsistsListFromFile(Context context) {
        Log.d(threaded_application.applicationName, activityName + ": loadRecentConsistsListFromFile(): Loading recent consists list from file");

        recentConsistLocoAddressList = new ArrayList<>();
        recentConsistAddressSizeList = new ArrayList<>();
        recentConsistDirectionList = new ArrayList<>();
        recentConsistLightList = new ArrayList<>();
        recentConsistSourceList = new ArrayList<>();
        recentConsistRosterNameList = new ArrayList<>();
        recentConsistNameList = new ArrayList<>();
        recentConsistNameHtmlList = new ArrayList<>();

        ArrayList<Integer> tempRecentConsistLocoAddressList_inner;
        ArrayList<Integer> tempRecentConsistAddressSizeList_inner;
        ArrayList<Integer> tempRecentConsistDirectionList_inner;
        ArrayList<Integer> tempRecentConsistSourceList_inner;
        ArrayList<String> tempRecentConsistRosterNameList_inner;
        ArrayList<Integer> tempRecentConsistLightList_inner;

        //if no SD Card present then there is no recent consists list
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            try {
                // Populate the List with the recent consists saved in a file. This
                // will be stored in recent_consist_list.txt
                File consist_list_file = new File(context.getExternalFilesDir(null), RECENT_CONSISTS_FILENAME);
                if (consist_list_file.exists()) {
                    BufferedReader list_reader = new BufferedReader(
                            new FileReader(consist_list_file));
                    while (list_reader.ready()) {
                        StringBuilder oneConsist = new StringBuilder();
                        StringBuilder oneConsistHtml = new StringBuilder();

                        String line = list_reader.readLine();
                        tempRecentConsistLocoAddressList_inner = new ArrayList<>();
                        tempRecentConsistAddressSizeList_inner = new ArrayList<>();
                        tempRecentConsistDirectionList_inner = new ArrayList<>();
                        tempRecentConsistSourceList_inner = new ArrayList<>();
                        tempRecentConsistRosterNameList_inner = new ArrayList<>();
                        tempRecentConsistLightList_inner = new ArrayList<>();

                        String consistName = "";
                        String splitOn = "<~>";
                        if (!line.contains("<~>")) { // must be the old format
                            splitOn = "~";
                        }
                        String[] splitLine = line.split(splitOn, -1);

                        if (splitLine.length > 1) { // see if there is a name saved as well
                            consistName = splitLine[1];
                            line = splitLine[0];
                            if (splitLine.length > 2) {  // see if there is roster names saved as well
                                String[] rosterNames = splitLine[2].split("<,>", -1);
                                tempRecentConsistRosterNameList_inner.addAll(Arrays.asList(rosterNames));
                            }
                        }

                        int splitLoco = line.indexOf(',');
                        if (splitLoco != -1) {
                            oneConsist.append(addOneConsistAddress(line, 0, splitLoco,
                                    tempRecentConsistLocoAddressList_inner,
                                    tempRecentConsistAddressSizeList_inner,
                                    tempRecentConsistDirectionList_inner,
                                    tempRecentConsistSourceList_inner,
                                    tempRecentConsistLightList_inner));
                            oneConsistHtml.append(addOneConsistAddressHtml(
                                    tempRecentConsistLocoAddressList_inner.get(0),
                                    tempRecentConsistAddressSizeList_inner.get(0)
                            ));

                            boolean foundOne = true;
                            while (foundOne) {
                                Integer prevSplitLoco = splitLoco + 1;
                                splitLoco = line.indexOf(',', prevSplitLoco);
                                if (splitLoco != -1) {
                                    oneConsist.append(addOneConsistAddress(line, prevSplitLoco, splitLoco,
                                            tempRecentConsistLocoAddressList_inner,
                                            tempRecentConsistAddressSizeList_inner,
                                            tempRecentConsistDirectionList_inner,
                                            tempRecentConsistSourceList_inner,
                                            tempRecentConsistLightList_inner));
                                } else {
                                    oneConsist.append(addOneConsistAddress(line, prevSplitLoco, line.length(),
                                            tempRecentConsistLocoAddressList_inner,
                                            tempRecentConsistAddressSizeList_inner,
                                            tempRecentConsistDirectionList_inner,
                                            tempRecentConsistSourceList_inner,
                                            tempRecentConsistLightList_inner));
                                    foundOne = false;
                                }
                            }
                            if (splitLine.length < 3) {  // old format - need to add some dummy roster names
                                for (int j = 0; j < tempRecentConsistLocoAddressList_inner.size(); j++) {
                                    tempRecentConsistRosterNameList_inner.add("");
                                }
                            }
                            if (consistName.isEmpty()) {
                                consistName = oneConsist.toString();
                            }
                            addRecentConsistToList(consistName,
                                    tempRecentConsistLocoAddressList_inner,
                                    tempRecentConsistAddressSizeList_inner,
                                    tempRecentConsistDirectionList_inner,
                                    tempRecentConsistSourceList_inner,
                                    tempRecentConsistRosterNameList_inner,
                                    tempRecentConsistLightList_inner);
                        }
                    }
                    list_reader.close();
                    Log.d(threaded_application.applicationName, activityName + ": loadRecentConsistsListFromFile(): Read recent consists list from file completed successfully");
                }

            } catch (IOException except) {
                Log.e(threaded_application.applicationName, activityName + ": loadRecentConsistsListFromFile(): Error reading recent consist file. "
                        + except.getMessage());
            }
        }
    }

    public void addRecentLocoToList(int address, int size, String locoName, int source, String functions) {
        addRecentLocoToList(-1, address, size, locoName, source, functions);
    }
    public void addRecentLocoToList(int atPosition, int address, int size, String locoName, int source, String functions) {
        if (atPosition<0) { // at end
            recentLocoAddressList.add(address);
            recentLocoAddressSizeList.add(size);
            recentLocoNameList.add(locoName);
            recentLocoSourceList.add(source);
            recentLocoFunctionsList.add(functions);
        } else {
            recentLocoAddressList.add(atPosition, address);
            recentLocoAddressSizeList.add(atPosition, size);
            recentLocoNameList.add(atPosition, locoName);
            recentLocoSourceList.add(atPosition, source);
            recentLocoFunctionsList.add(atPosition, functions);
        }
    }

    public void removeRecentLocoFromList(int position) {
        recentLocoAddressList.remove(position);
        recentLocoAddressSizeList.remove(position);
        recentLocoNameList.remove(position);
        recentLocoSourceList.remove(position);
        recentLocoFunctionsList.remove(position);
    }

    public String findRecentLocoFunctions(int locoAddress, int locoAddressSize, String locoName) {
        // check if it is already in the list and remove it
        String functions = "";
        for (int i = 0; i < recentLocoAddressList.size(); i++) {
            Log.d(threaded_application.applicationName, activityName + ": findRecentLocoEntry(): locoName='"+locoName+"', address=" + locoAddress);
            if (locoAddress == recentLocoAddressList.get(i)
                    && locoAddressSize == recentLocoAddressSizeList.get(i)
                    && locoName.equals(recentLocoNameList.get(i))) {

                functions = recentLocoFunctionsList.get(i);
                Log.d(threaded_application.applicationName, activityName + ": findRecentLocoEntry(): Loco '"+ locoName + "' removed from Recents");
                break;
            }
        }
        return functions;
    }


    private String addOneConsistAddress(String line, Integer start, Integer end,
                                        ArrayList<Integer> tempRecentConsistLocoAddressList_inner,
                                        ArrayList<Integer> tempRecentConsistAddressSizeList_inner,
                                        ArrayList<Integer> tempRecentConsistDirectionList_inner,
                                        ArrayList<Integer> tempRecentConsistSourceList_inner,
                                        ArrayList<Integer> tempRecentConsistLightList_inner) {
        String rslt = "";
        String splitLine = line.substring(start, end);
        int splitPos = splitLine.indexOf(':');
        if (splitPos!=-1) {
            Integer addr = Integer.decode(splitLine.substring(0, splitPos));
            int size = Integer.decode(splitLine.substring(splitPos + 1, splitPos + 2));
            int dir = Integer.decode(splitLine.substring(splitPos + 2, splitPos + 3));
            int source = source_type.UNKNOWN; //default to unknown
            int light = light_follow_type.UNKNOWN; //default to unknown
            if (splitLine.length()>splitPos + 3) {  // if short, then this is the first format that did not include the source or light value
                source = Integer.decode(splitLine.substring(splitPos + 3, splitPos + 4));
                light = Integer.decode(splitLine.substring(splitPos + 4, splitPos + 5));
            }
            tempRecentConsistLocoAddressList_inner.add(addr);
            tempRecentConsistAddressSizeList_inner.add(size);
            tempRecentConsistDirectionList_inner.add(dir);
            tempRecentConsistSourceList_inner.add(source);
            tempRecentConsistLightList_inner.add(light);

            rslt = addr.toString();
        }
        return rslt;
    }

    public String addOneConsistAddressHtml(Integer addr, int size) {
        return "<span>" + addr.toString()
                +"<small><small>("+ (size==0 ? "S":"L") +")"  + "</small></small>"
//                + "<small><small>" + (dir==0 ? "▲":"▼") + "</small></small>"
//                +  (light==light_follow_type.OFF ? "○": (light==light_follow_type.FOLLOW ? "●":"<small><small>?</small></small>"))
//                +  getSourceHtmlString(source)
                + " &nbsp;</span>";
    }

    public void addRecentConsistToList(String Name,
                                       ArrayList<Integer> LocoAddress,
                                       ArrayList<Integer> AddressSize,
                                       ArrayList<Integer> Direction,
                                       ArrayList<Integer> Source,
                                       ArrayList<String>  RosterName,
                                       ArrayList<Integer> Light) {
        addRecentConsistToList(-1, Name, LocoAddress, AddressSize, Direction, Source, RosterName, Light);
    }
    public void addRecentConsistToList(int atPosition,
                                       String Name,
                                       ArrayList<Integer> locoAddress,
                                       ArrayList<Integer> addressSize,
                                       ArrayList<Integer> direction,
                                       ArrayList<Integer> source,
                                       ArrayList<String>  rosterName,
                                       ArrayList<Integer> light) {

        StringBuilder oneConsistHtml = new StringBuilder();
        for (int j = 0; j < locoAddress.size(); j++) {
            oneConsistHtml.append(addOneConsistAddressHtml(
                    locoAddress.get(j),
                    addressSize.get(j)
            ));
        }

        if (atPosition<0) { // add to end
            recentConsistNameList.add(Name);
            recentConsistNameHtmlList.add(oneConsistHtml.toString());
            recentConsistLocoAddressList.add(locoAddress);
            recentConsistAddressSizeList.add(addressSize);
            recentConsistDirectionList.add(direction);
            recentConsistSourceList.add(source);
            recentConsistRosterNameList.add(rosterName);
            recentConsistLightList.add(light);
        } else {
            recentConsistNameList.add(atPosition, Name);
            recentConsistNameHtmlList.add(atPosition, oneConsistHtml.toString());
            recentConsistLocoAddressList.add(atPosition, locoAddress);
            recentConsistAddressSizeList.add(atPosition, addressSize);
            recentConsistDirectionList.add(atPosition, direction);
            recentConsistSourceList.add(atPosition, source);
            recentConsistRosterNameList.add(atPosition, rosterName);
            recentConsistLightList.add(atPosition, light);
        }
    }

    public void removeRecentConsistFromListAtPosition(int position) {
        recentConsistLocoAddressList.remove(position);
        recentConsistAddressSizeList.remove(position);
        recentConsistDirectionList.remove(position);
        recentConsistSourceList.remove(position);
        recentConsistRosterNameList.remove(position);
        recentConsistLightList.remove(position);
        recentConsistNameList.remove(position);
        recentConsistNameHtmlList.remove(position);

    }

    public int addCurrentConsistToBeginningOfList(Consist consist) { // if necessary   return -1 if not currently in the list
        ArrayList<Integer> tempRecentConsistLocoAddressList_inner = new ArrayList<>();
        ArrayList<Integer> tempRecentConsistAddressSizeList_inner = new ArrayList<>();
        ArrayList<Integer> tempRecentConsistDirectionList_inner = new ArrayList<>();
        ArrayList<Integer> tempRecentConsistSourceList_inner = new ArrayList<>();
        ArrayList<String> tempRecentConsistRosterNameList_inner = new ArrayList<>();
        ArrayList<Integer> tempRecentConsistLightList_inner = new ArrayList<>();

        //if not updating list or no SD Card present then nothing else to do
        if ( (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            || (consist == null) )
            return -1;

        Collection<Consist.ConLoco> conLocos = consist.getLocos();


        for (Consist.ConLoco l : conLocos) {
            tempRecentConsistLocoAddressList_inner.add(l.getIntAddress());
            tempRecentConsistAddressSizeList_inner.add(l.getIntAddressLength());
            String addr = locoAddressToString(l.getIntAddress(), l.getIntAddressLength(), true);
            tempRecentConsistDirectionList_inner.add((consist.isBackward(addr) ? 1 : 0));
            String rosterName = "";
            if (l.getRosterName() != null) {
                rosterName = l.getRosterName();
            }
//            tempRecentConsistSourceList_inner.add(rosterName.equals("") ? source_type.ADDRESS : source_type.ROSTER);
            tempRecentConsistSourceList_inner.add(l.getWhichSource());
            tempRecentConsistRosterNameList_inner.add(rosterName);
            tempRecentConsistLightList_inner.add((consist.isLight(addr)));
        }

        int whichEntryIsBeingUpdated = -1;
        // find out which entry it is in the roster
        boolean isSame = false;
        for (int i = 0; i < recentConsistLocoAddressList.size() && !isSame; i++) {
            if (recentConsistLocoAddressList.get(i).size() == tempRecentConsistLocoAddressList_inner.size()) {  // if the lists are different sizes don't bother
                for (int j = 0; j < recentConsistLocoAddressList.get(i).size() && !isSame; j++) {
                    if ((recentConsistLocoAddressList.get(i).get(j).equals(tempRecentConsistLocoAddressList_inner.get(j)))
//                            && (consistDirectionList.get(i).get(j).equals(tempRecentConsistDirectionList_inner.get(j)))
                    ) {
                        isSame = true;
                    }
                }
                if (isSame) {
                    whichEntryIsBeingUpdated = i + 1; //remember this, so we can remove this line in the list.  Add 1 because we are gone to force a new line at the top
                }
            }
        }

        String consistName = consist.toString();
        if (whichEntryIsBeingUpdated>0) { //this may already have a name
            consistName = recentConsistNameList.get(whichEntryIsBeingUpdated-1);
        }

        //add it to the beginning of the list
        addRecentConsistToList(0,consistName,
                                        tempRecentConsistLocoAddressList_inner,
                                        tempRecentConsistAddressSizeList_inner,
                                        tempRecentConsistDirectionList_inner,
                                        tempRecentConsistSourceList_inner,
                                        tempRecentConsistRosterNameList_inner,
                                        tempRecentConsistLightList_inner);

        return whichEntryIsBeingUpdated;
    }


    public void writeRecentConsistsListToFile(Context context, SharedPreferences sharedPreferences, int whichEntryIsBeingUpdated) {
        Log.d(threaded_application.applicationName, activityName + ": writeRecentConsistsListToFile(): Writing recent consists list to file");

        // write it out from the saved preferences to the file
        File consist_list_file = new File(context.getExternalFilesDir(null), RECENT_CONSISTS_FILENAME);

        PrintWriter list_output;
        int numberOfRecentLocosToWrite = getIntPrefValue(sharedPreferences, "prefMaximumRecentLocos", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue));
        try {
            list_output = new PrintWriter(consist_list_file);
            if (numberOfRecentLocosToWrite > 0) {
                for (int i = 0; i < recentConsistNameList.size() && numberOfRecentLocosToWrite > 0; i++) {

                    if (i!=whichEntryIsBeingUpdated) { // if this is the one being updated, don't write it

                        for (int j = 0; j < recentConsistAddressSizeList.get(i).size(); j++) {
                            if (j > 0) {
                                list_output.format(",");
                            }
                            list_output.format("%d:%d%d%d%d",
                                    recentConsistLocoAddressList.get(i).get(j),
                                    recentConsistAddressSizeList.get(i).get(j),
                                    recentConsistDirectionList.get(i).get(j),
                                    recentConsistSourceList.get(i).get(j),
                                    (j==0 ? light_follow_type.FOLLOW :recentConsistLightList.get(i).get(j)) );  // always set the first loco as 'follow'
                        }
                        list_output.format("<~>%s<~>", recentConsistNameList.get(i));
                        for (int j = 0; j < recentConsistRosterNameList.get(i).size(); j++) {
                            if (j > 0) {
                                list_output.format("<,>");
                            }
                            list_output.format("%s",
                                    recentConsistRosterNameList.get(i).get(j));
                        }
                        // note recentConsistNameHtmlList is not save or loaded. it is generated as needed

                        list_output.format("\n");
                        numberOfRecentLocosToWrite--;
                    }
                }
            }
            list_output.flush();
            list_output.close();
            Log.d(threaded_application.applicationName, activityName + ": writeRecentConsistsListToFile(): Write recent consists list to file completed successfully");
        } catch (IOException except) {
            Log.e(threaded_application.applicationName, activityName + ": writeRecentConsistsListToFile(): Error creating a PrintWriter, IOException: "
                            + except.getMessage());
        }
    }

    @SuppressLint("DefaultLocale")
    public void writeThrottlesEnginesListToFile(threaded_application mainapp, Context context, int numThrottles) {
        Log.d(threaded_application.applicationName, activityName + ": writeThrottlesEnginesListToFile(): Writing throttles engines list to file");

        File throttles_engines_list_file = new File(context.getExternalFilesDir(null), THROTTLES_ENGINES_FILENAME);

        PrintWriter list_output;
        try {
            list_output = new PrintWriter(throttles_engines_list_file);
            if (numThrottles > 0) {
                for (int whichThrottle = 0; whichThrottle < numThrottles; whichThrottle++) {
                    Consist con = mainapp.consists[whichThrottle];

                    StringBuilder throttleLocoStrBuilder = new StringBuilder();
                    for (Consist.ConLoco l : con.getLocos()) {
                        int dir = l.isBackward() ? 1 : 0;
                        String name = l.getFormatAddress();
                        if (l.getRosterName() != null) name = l.getRosterName();
                        throttleLocoStrBuilder.append(String.format("%d<,>%s<,>%s<,>%d<,>%d<;>",
                                l.getIntAddress(), name, l.getAddress(), l.getWhichSource(), dir));
                    }
//                    list_output.format("%s<~>%d%s", mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_"), whichThrottle, throttleLocoStrBuilder.toString());
                    list_output.format("%d<~>%s", whichThrottle, throttleLocoStrBuilder.toString());
                    list_output.format("\n");
                }
            }
            list_output.flush();
            list_output.close();
            Log.d(threaded_application.applicationName, activityName + ": writeThrottlesEnginesListToFile(): Write throttles engines list to file completed successfully");
        } catch (IOException except) {
            Log.e(threaded_application.applicationName, activityName + ": writeThrottlesEnginesListToFile(): Error creating a PrintWriter, IOException: "
                            + except.getMessage());
        }
    }

    public void loadThrottlesEnginesListFromFile(threaded_application mainapp, Context context, int numThrottles) {
        Log.d(threaded_application.applicationName, activityName + ": loadThrottlesEnginesListFromFile(): Reading throttles engines list from file");

        File throttles_engines_list_file = new File(context.getExternalFilesDir(null), THROTTLES_ENGINES_FILENAME);
        if (throttles_engines_list_file.exists()) {
            try {
                BufferedReader list_reader = new BufferedReader(
                        new FileReader(throttles_engines_list_file));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    String[] splitLine = line.split("<~>", -1);
                    int whichThrottle = Integer.parseInt(splitLine[0]);
                    if (!splitLine[1].isEmpty()) { // has locos
                        String[] splitLocos = splitLine[1].split("<;>", -1);
                        int numberInConsist = splitLocos.length;
                        for ( int i=0; i<numberInConsist; i++ ) {
                            if (!splitLocos[i].isEmpty()) {
                                String[] splitOneLoco = splitLocos[i].split("<,>", -1);
                                int address = Integer.parseInt(splitOneLoco[0]);
                                String locoName = splitOneLoco[1];
                                String locoAddress = splitOneLoco[2];
                                int locoSource = Integer.parseInt(splitOneLoco[3]);
                                boolean locoIsBackwards = (Integer.parseInt(splitOneLoco[4]) == 1) ? true : false;
                                Loco l = new Loco(locoAddress);
                                l.setDesc(locoName);
                                if (locoSource != source_type.ADDRESS) {
//                                    locoName = mainapp.findLocoNameInRoster(locoName);  // confirm that the loco is actually in the roster
                                    l.setRosterName(locoName);
                                    l.setIsFromRoster(true);
                                } else {
                                    l.setRosterName(null); //make sure rosterName is null
                                }

                                // see if we can find it in the recents list and load the function labels
                                if (recentLocoAddressList == null) {
                                    loadRecentLocosListFromFile(context);
                                }
                                boolean found = false;
                                String functions = "";
                                if ( (recentLocoAddressList != null) && (!recentLocoAddressList.isEmpty()) ) {
                                    for (int j = 0; j < recentLocoAddressList.size(); j++) {
                                        if (recentLocoAddressList.get(j) == address) {
                                            functions = recentLocoFunctionsList.get(j);
                                            l.setFunctionLabels(recentLocoFunctionsList.get(j));
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                                if (!found) {
//                                    l.setFunctionLabels("");
                                    l.setFunctionLabelDefaults(mainapp, whichThrottle);
                                }

                                Consist consist = mainapp.consists[whichThrottle];

                                boolean result = true;
                                // if we already have it show message and request it anyway
                                if (!consist.isEmpty()) {
                                    for (int j = 0; j <= consist.size(); j++) {
                                        if (consist.getLoco(locoAddress) != null) {
                                            mainapp.safeToast(context.getResources().getString(R.string.toastLocoAlreadySelected, locoAddress), Toast.LENGTH_SHORT);
                                            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, locoAddress, whichThrottle);  // send the acquire message anyway
                                            result = false;
                                            break;
                                        }
                                    }
                                }

                                if (result) {
                                    consist.add(l);
                                    consist.setWhichSource(locoAddress, locoSource);
                                    consist.setTrailAddr(l.getAddress());
                                    if (i == 0) {
                                        consist.setLeadAddr(l.getAddress());
                                    }
                                    consist.setBackward(locoAddress, locoIsBackwards);
                                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, locoAddress, whichThrottle);


                                    if ( (i==0) && (found) ) { //first only
                                        if (!functions.isEmpty()) {
                                            LinkedHashMap<Integer, String> functionLabelsMap = threaded_application.parseFunctionLabels("RF29}|{1234(L)]\\[" + functions);  //prepend some stuff to match old-style
                                            mainapp.function_labels[whichThrottle] = functionLabelsMap;
                                        } else {
                                            mainapp.function_labels[whichThrottle] = new LinkedHashMap<>(mainapp.function_labels_default);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                list_reader.close();
                Log.d(threaded_application.applicationName, activityName + ": loadThrottlesEnginesListFromFile(): Read throttles engines list from file completed successfully");

            } catch(IOException except){
                Log.e(threaded_application.applicationName, activityName + ": loadThrottlesEnginesListFromFile(): Error reading throttles engines list. "
                        + except.getMessage());
            }
        }
    }

        @SuppressLint("ApplySharedPref")
    private void saveIntListDataToPreferences(ArrayList<Integer> list, String listName, SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt(listName +"_size", list.size()).commit();
        int prefInt;
        for(int i=0 ; i<list.size() ; i++){
            prefInt=list.get(i);
            sharedPreferences.edit().putInt(listName + "_" + i, prefInt).commit();
        }
        sharedPreferences.edit().commit();
    }

    @SuppressLint("ApplySharedPref")
    private int removeExtraListDataFromPreferences(int startFrom, int stopAt, String listName, SharedPreferences sharedPreferences) {
        int prefCount = 0;
        if (startFrom==0) {   // startFrom = zero will clear all items
            sharedPreferences.edit().remove(listName + "_size").commit();
        }
        for(int i=startFrom ; i<stopAt ; i++){
            try {
                String prefName = listName + "_" + i;
                if (sharedPreferences.contains(prefName)) {
                    sharedPreferences.edit().remove(prefName).commit();
                    prefCount = i + 1;
                }
            } catch (Exception except) {
                //ignore it
            }
        }
        sharedPreferences.edit().commit();

//        Log.d(threaded_application.applicationName, activityName + ": writeRecentConsistsListToFile(): removeExtraListDataFromPreferences: list: " +listName + " prefCount" + prefCount);

        return prefCount;
    }


    private void getIntListDataFromPreferences(ArrayList<Integer> list, String listName, SharedPreferences sharedPreferences, int forceSize, int defaultValue) {
        int size = sharedPreferences.getInt(listName + "_size", 0);
        if (forceSize>0) { // get a specified number regardless of how many are stored
            size = forceSize;
        }
        int prefInt;
        for(int i=0 ; i<size ; i++){
            prefInt = sharedPreferences.getInt(listName + "_" + i, defaultValue);
            list.add(prefInt);
        }
    }

    @SuppressLint("ApplySharedPref")
    private void saveStringListDataToPreferences(ArrayList<String> list, String listName, SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt(listName +"_size", list.size()).commit();
        String prefString;
        for(int i=0 ; i<list.size() ; i++){
            prefString = list.get(i);
            sharedPreferences.edit().putString(listName + "_" + i, prefString).commit();
        }
        sharedPreferences.edit().commit();
    }

    private void getStringListDataFromPreferences(ArrayList<String> list, String listName, SharedPreferences sharedPreferences, int forceSize, String defaultValue) {
        int size = sharedPreferences.getInt(listName + "_size", 0);
        if (forceSize>0) { // get a specified number regardless of how many are stored
            size = forceSize;
        }
        String prefString;
        for(int i=0 ; i<size ; i++){
            prefString = sharedPreferences.getString(listName + "_" + i, defaultValue);
            list.add(prefString);
        }
    }

    public String locoAddressToString(Integer addr, int size, boolean sizeAsPrefix) {
        String engineAddressString = "";
        try {
            String addressLengthString = ((size == 0) ? "S" : "L");  //show L or S based on length from file
            if (!sizeAsPrefix) {
                engineAddressString = String.format("%s(%s)", addr.toString(), addressLengthString);  //e.g.  1009(L)
            } else {
                engineAddressString = addressLengthString + addr.toString();  //e.g.  L1009
            }
        } catch (Exception e) {
            Log.e(threaded_application.applicationName, activityName + ":  locoAddressToString() Exception: " + e);
        }
        return engineAddressString;
    }

    public String locoAddressToHtml(Integer addr, int size, int source) {
        String engineAddressHtml = "";
        try {
            String addressLengthString = ((size == 0) ? "S" : "L");  //show L or S based on length from file
//            String addressSourceString = getSourceHtmlString(source);
//            engineAddressHtml = String.format("<span>%s<small>(%s)</small>%s </span>", addr.toString(), addressLengthString, addressSourceString);
            engineAddressHtml = String.format("<span>%s<small>(%s)</small> </span>", addr.toString(), addressLengthString);
        } catch (Exception e) {
            Log.e(threaded_application.applicationName, activityName + ":  locoAddressToHtml() Exception: " + e);
        }
        return engineAddressHtml;
    }


//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public void writeRecentTurnoutsListToFile(Context context, SharedPreferences sharedPreferences) {
//        Log.d(threaded_application.applicationName, activityName + ": writeRecentTurnoutsListToFile(): Writing recent turnouts list to file");

        File engine_list_file = new File(context.getExternalFilesDir(null), RECENT_TURNOUTS_FILENAME);

        PrintWriter list_output;
        String smrl = sharedPreferences.getString("prefMaximumRecentLocos", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue)); //retrieve pref for max recent locos to show
        try {
            int numberOfRecentTurnoutsToWrite = Integer.parseInt(smrl) * 2;
            numberOfRecentTurnoutsToWrite = min(numberOfRecentTurnoutsToWrite, recentTurnoutAddressList.size());
            list_output = new PrintWriter(engine_list_file);
            if (numberOfRecentTurnoutsToWrite > 0) {
//                for (int i = 0; i < recentTurnoutAddressList.size() && numberOfRecentTurnoutsToWrite > 0; i++) {
                for (int i = 0; i < numberOfRecentTurnoutsToWrite; i++) {
                    list_output.format("%s:%d<~>%s<~>%s\n",
                            recentTurnoutAddressList.get(i),
                            recentTurnoutSourceList.get(i),
                            recentTurnoutServerList.get(i),
                            recentTurnoutNameList.get(i));
//                    numberOfRecentTurnoutsToWrite--;
                }
            } else {
                deleteFile(context, RECENT_TURNOUTS_FILENAME);
                return;
            }
            list_output.flush();
            list_output.close();
            Log.d(threaded_application.applicationName, activityName + ": writeRecentTurnoutsListToFile(): " +
                    "Write recent turnouts list to file completed successfully with " + recentTurnoutAddressList.size() + " entries.");
        } catch (IOException except) {
            Log.e(threaded_application.applicationName, activityName + ": writeRecentTurnoutsListToFile(): Error creating a PrintWriter, IOException: "
                            + except.getMessage());
        } catch (IndexOutOfBoundsException except) {
            Log.e(threaded_application.applicationName, activityName + ": writeRecentTurnoutsListToFile(): Error writing recent turnouts lists, IndexOutOfBoundsException: "
                            + except.getMessage());
        }
    }

    /** @noinspection UnusedReturnValue*/
    public boolean deleteFile(Context context, String filename) {
        Log.d(threaded_application.applicationName, activityName + ": deleteFile():");

        File file = new File(context.getExternalFilesDir(null), filename);
        if (file.exists()) {
            try {
                file.delete();
                return(true);
            } catch (Exception except) {
                Log.e(threaded_application.applicationName, activityName + ": deleteRecentTurnoutsListFile(): Error deleting : " + filename
                                + except.getMessage());
            }
        }
        return(false);
    }

        public void loadRecentTurnoutsListFromFile(Context context) {
//        Log.d(threaded_application.applicationName, activityName + ": loadRecentTurnoutsListFromFile(): Loading recent turnouts list from file");
        try {
            // Populate the List with the recent engines saved in a file. This
            // will be stored in recent_engine_list.txt
            File turnouts_list_file = new File(context.getExternalFilesDir(null), RECENT_TURNOUTS_FILENAME);
            if (turnouts_list_file.exists()) {
                BufferedReader list_reader = new BufferedReader(
                        new FileReader(turnouts_list_file));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    int splitPos = line.indexOf(':');
                    if (splitPos > 0) {
                        Integer source;
                        String addr, turnoutName ="", turnoutServer = "";
                        try {
                             addr = line.substring(0, splitPos);
                            source = Integer.decode(line.substring(splitPos + 1, splitPos + 2));
                            if (line.length()>splitPos+2) { // has the name extras
//                                if (line.substring(splitPos + 2,splitPos + 3).equals("~")) { // is the early format without the server
//                                    turnoutName = line.substring(splitPos + 3);
//                                }
                                if (!line.contains("<~>")) { // must be the old format
                                    String[] splitLine = line.split("~", -1);
                                    turnoutName = splitLine[1];
                                } else {
                                    String[] splitLine = line.split("<~>", -1);
                                    turnoutServer = splitLine[1];
                                    turnoutName = splitLine[2];
                                }
                            }
                        } catch (Exception e) {
                            addr = "";
                            turnoutName = "";
                            source = -1;
                        }
                        if (!addr.isEmpty()) {
                            recentTurnoutAddressList.add(addr);
                            recentTurnoutNameList.add(turnoutName);
                            recentTurnoutSourceList.add(source);
                            recentTurnoutServerList.add(turnoutServer);

                        }
                    }
                }
                list_reader.close();
            }
            Log.d(threaded_application.applicationName, activityName + ": loadRecentTurnoutsListFromFile(): " +
                    "Read recent turnouts list from file completed successfully with " + recentTurnoutAddressList.size() + " entries.");

        } catch (IOException except) {
            Log.e(threaded_application.applicationName, activityName + ": loadRecentTurnoutsListFromFile(): Error reading recent turnouts file. "
                    + except.getMessage());
        }
    }

    private int getIntPrefValue(SharedPreferences sharedPreferences, String key, String defaultVal) {
        int newVal;
        try {
            newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
        } catch (NumberFormatException e) {
            try {
                newVal = Integer.parseInt(defaultVal);
            } catch (NumberFormatException ex) {
                newVal = 0;
            }
        }
        return newVal;
    }

    @SuppressLint({"DefaultLocale", "ApplySharedPref"})
    public void downloadRosterToRecents(Context context, SharedPreferences sharedPreferences, threaded_application mainapp) {
        loadRecentLocosListFromFile(context);

        ArrayList<String> rns = new ArrayList<>(mainapp.roster_entries.keySet());  //copy from synchronized map to avoid holding it while iterating
        int recentsSize = getIntPrefValue(sharedPreferences, "prefMaximumRecentLocos", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue));
        int requiredRecentsSize = rns.size()+ 10;
        if (requiredRecentsSize>recentsSize) { // force the preference for the max recents to larger than the roster
            sharedPreferences.edit().putString("prefMaximumRecentLocos", String.format("%d",requiredRecentsSize)).commit();  //reset the preference
        }

        int j=0;
        for (String rostername : rns) {
            HashMap<String, String> hm = mainapp.rosterFullList.get(j);
            String rosterNameString = hm.get("roster_name");
            String rosterAddressString = hm.get("roster_address");
            String rosterEntryType = hm.get("roster_entry_type");

            String locoName = "";
            int locoAddressSize = 0;
            int locoAddress = 0;

            // parse address and length from string, e.g. 2591(L)
            String[] ras = threaded_application.splitByString(rosterAddressString, "(");
            if (!ras[0].isEmpty()) {  //only process if address found
                locoAddressSize = (ras[1].charAt(0) == 'L') ? address_type.LONG : address_type.SHORT;   // convert S/L to 0/1
                try {
                    locoAddress = Integer.parseInt(ras[0]);   // convert address to int
                } catch (NumberFormatException e) {
                    mainapp.safeToast(context.getResources().getString(R.string.toastImportExportCouldNotParseAddress, e.getMessage()), Toast.LENGTH_SHORT);
                    return; //get out, don't try to acquire
                }
                if ("loco".equals(rosterEntryType)) {
                    locoName = rosterNameString;
                }
            }

            // check if it is already in the list and remove it
            String keepFunctions = "";
            for (int i = 0; i < recentLocoAddressList.size(); i++) {
                Log.d(threaded_application.applicationName, activityName + ": downloadRosterToRecents(): locoName='"+locoName+"', address=" + locoAddress);
                if (locoAddress == recentLocoAddressList.get(i)
                        && locoAddressSize == recentLocoAddressSizeList.get(i)
                        && locoName.equals(recentLocoNameList.get(i))) {

                    keepFunctions = recentLocoFunctionsList.get(i);
                    removeRecentLocoFromList(i);
                    Log.d(threaded_application.applicationName, activityName + ": downloadRosterToRecents(): Loco '"+ locoName + "' removed from Recents");
                    break;
                }
            }

            // now append it to the end of the list
            addRecentLocoToList(locoAddress, locoAddressSize, locoName, source_type.ROSTER, keepFunctions);   // functions are blank for now

            Log.d(threaded_application.applicationName, activityName + ": downloadRosterToRecents(): Loco '"+ locoName + "' added to Recents");

            j++;
        }

        writeRecentLocosListToFile(context, sharedPreferences);

        // now get all the function labels
        j=0;
        for (String rostername : rns) {
            String sAddr = locoAddressToString(recentLocoAddressList.get(j),
                    recentLocoAddressSizeList.get(j), true);
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, sAddr, mainapp.maxThrottles);  // one past the actual number of allow throttles
            j++;
        }
    }

}
