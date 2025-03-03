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

 * This helper class for importing and exporting shared preferences to SD card files.
 * Created by Peter Akers on 17-Dec-17.
*/

package jmri.enginedriver.import_export;

import static java.lang.Math.min;
import static jmri.enginedriver.threaded_application.context;

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
import java.util.Map;

import jmri.enginedriver.type.Consist;
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

    public boolean currentlyImporting = false;

    public static final String RECENT_TURNOUTS_FILENAME = "recent_turnouts_list.txt";
    private static final String RECENT_CONSISTS_FILENAME = "recent_consist_list.txt";
    private static final String RECENT_ENGINES_FILENAME = "recent_engine_list.txt";

    public ArrayList<Integer> recentLocoAddressList;
    public ArrayList<Integer> recentLocoAddressSizeList; // Look at address_type.java
    public ArrayList<String> recentLocoNameList;
//    ArrayList<String> recentLocoNameHtmlList;
    public ArrayList<Integer> recentLocoSourceList;
    public ArrayList<String> recentLocoFunctionsList;

    public ArrayList<ArrayList<Integer>> consistEngineAddressList = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> consistAddressSizeList = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> consistDirectionList = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> consistSourceList = new ArrayList<>();
    public ArrayList<ArrayList<String>> consistRosterNameList = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> consistLightList = new ArrayList<>();  // placeholder - not currently use
    public ArrayList<String> consistNameList = new ArrayList<>();
    public ArrayList<String> consistNameHtmlList = new ArrayList<>();

    public ArrayList<String> recentTurnoutAddressList;
    public ArrayList<String> recentTurnoutNameList;
    public ArrayList<Integer> recentTurnoutSourceList;
    public ArrayList<String> recentTurnoutServerList;

    private boolean writeExportFile(Context context, SharedPreferences sharedPreferences, String exportedPreferencesFileName){
        Log.d("Engine_Driver", "writeExportFile: ImportExportPreferences: Writing export file");
        boolean result = false;
        ObjectOutputStream output = null;

        File dst = new File(context.getExternalFilesDir(null), exportedPreferencesFileName);

        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            output.writeObject(sharedPreferences.getAll());
            @SuppressLint("StringFormatMatches") String m = context.getResources().getString(R.string.toastImportExportExportSucceeded,exportedPreferencesFileName);
//            Toast.makeText(context, m, Toast.LENGTH_SHORT).show();
            threaded_application.safeToast(m, Toast.LENGTH_SHORT);
            Log.d("Engine_Driver", m);
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
            Log.e("Engine_Driver", "writeExportFile: ImportExportPreferences: Export Failed");
//            Toast.makeText(context, "Export failed!", Toast.LENGTH_LONG).show();
            threaded_application.safeToast(R.string.toastImportExportExportFailed, Toast.LENGTH_LONG);
        } else {
            Log.d("Engine_Driver", "writeExportFile: ImportExportPreferences: Export succeeded");

        }
        return result;
    }

    public void writeSharedPreferencesToFile(Context context, SharedPreferences sharedPreferences, String exportedPreferencesFileName) {
        Log.d("Engine_Driver", "writeSharedPreferencesToFile: ImportExportPreferences: Saving preferences to file");

        boolean prefImportExportLocoList = sharedPreferences.getBoolean("prefImportExportLocoList", context.getResources().getBoolean(R.bool.prefImportExportLocoListDefaultValue));
        if (prefImportExportLocoList) {
            recentLocoAddressList = new ArrayList<>();
            recentLocoAddressSizeList = new ArrayList<>();
            recentLocoNameList = new ArrayList<>();
            recentLocoSourceList = new ArrayList<>();
            recentLocoFunctionsList = new ArrayList<>();
            loadRecentLocosListFromFile();
            saveIntListDataToPreferences(recentLocoAddressList, "prefRecentLoco", sharedPreferences);
            saveIntListDataToPreferences(recentLocoAddressSizeList, "prefRecentLocoSize", sharedPreferences);
            saveStringListDataToPreferences(recentLocoNameList, "prefRecentLocoName", sharedPreferences);
            saveIntListDataToPreferences(recentLocoSourceList, "prefRecentLocoSource", sharedPreferences);
            saveStringListDataToPreferences(recentLocoFunctionsList, "prefRecentLocoFunction", sharedPreferences);

            loadRecentConsistsListFromFile();
            saveStringListDataToPreferences(consistNameList, "prefRecentConsistName", sharedPreferences);
            for (int i = 0; i < consistNameList.size(); i++) {
                saveIntListDataToPreferences(consistEngineAddressList.get(i), "prefRecentConsistAddress_"+i, sharedPreferences);
                saveIntListDataToPreferences(consistAddressSizeList.get(i), "prefRecentConsistSize_"+i, sharedPreferences);
                saveIntListDataToPreferences(consistDirectionList.get(i), "prefRecentConsistDirection_"+i, sharedPreferences);
                saveIntListDataToPreferences(consistSourceList.get(i), "prefRecentConsistSource_"+i, sharedPreferences);
                saveStringListDataToPreferences(consistRosterNameList.get(i), "prefRecentConsistRosterName_"+i, sharedPreferences);
                saveIntListDataToPreferences(consistLightList.get(i), "prefRecentConsistLight_"+i, sharedPreferences);
            }
        }

        if (!exportedPreferencesFileName.equals(".ed")) {
            writeExportFile(context, sharedPreferences, exportedPreferencesFileName);
        } else {
//            Toast.makeText(context, context.getResources().getString(R.string.toastImportExportExportFailed), Toast.LENGTH_LONG).show();
            threaded_application.safeToast(R.string.toastImportExportExportFailed, Toast.LENGTH_LONG);
        }

        int numberOfRecentLocosToWrite = getIntPrefValue(sharedPreferences, "maximum_recent_locos_preference", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue));

        int prefCount;
        if (prefImportExportLocoList) {  // now clean out the preference data

            Log.d("Engine_Driver", "writeSharedPreferencesToFile:  Normal Cleanout of old Recent Locos preferences");
            prefCount = removeExtraListDataFromPreferences(0,numberOfRecentLocosToWrite+1,"prefRecentLoco", sharedPreferences);
            if (prefCount == numberOfRecentLocosToWrite+1) {  // if there were that many, assume the worst
                Log.d("Engine_Driver", "writeSharedPreferencesToFile:  Extended Cleanout of old Recent Locos preferences");
                prefCount = removeExtraListDataFromPreferences(numberOfRecentLocosToWrite+1, 600, "prefRecentLoco", sharedPreferences);
            }
            removeExtraListDataFromPreferences(0, prefCount,"prefRecentLocoSize", sharedPreferences);
            removeExtraListDataFromPreferences(0,prefCount,"prefRecentLocoName", sharedPreferences);
            removeExtraListDataFromPreferences(0,prefCount,"prefRecentLocoSource", sharedPreferences);

            prefCount =removeExtraListDataFromPreferences(0,numberOfRecentLocosToWrite+1,"prefRecentConsistName", sharedPreferences);
            if (prefCount == numberOfRecentLocosToWrite+1) {  // if there were that many, look for more
                Log.d("Engine_Driver", "writeSharedPreferencesToFile:  Extended Cleanout of old Recent Locos preferences");
                prefCount = removeExtraListDataFromPreferences(numberOfRecentLocosToWrite+1, numberOfRecentLocosToWrite+20, "prefRecentConsistName", sharedPreferences);

                if (prefCount == numberOfRecentLocosToWrite+20) {  // if there were that many, assume the worst
                    Log.d("Engine_Driver", "writeSharedPreferencesToFile:  Extended DEEP CLEAN of old Recent Locos preferences");
                    prefCount = removeExtraListDataFromPreferences(numberOfRecentLocosToWrite+1, 600, "prefRecentConsistName", sharedPreferences);
                }
            }

            Log.d("Engine_Driver", "writeSharedPreferencesToFile:  Normal Cleanout of old Recent Consist preferences");
            for (int i = 0; i < numberOfRecentLocosToWrite; i++) {
                int subPrefCount = removeExtraListDataFromPreferences(0,10,"prefRecentConsistAddress_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistSize_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistDirection_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistSource_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistRosterName_"+i, sharedPreferences);
                removeExtraListDataFromPreferences(0,subPrefCount,"prefRecentConsistLight_"+i, sharedPreferences);
            }

            if (sharedPreferences.contains("prefRecentTurnoutServer_0")) {  // there should not be any so assume the worst
                Log.d("Engine_Driver", "writeSharedPreferencesToFile:  Extended Cleanout of old Recent turnouts preferences - these should not exist");
                prefCount = removeExtraListDataFromPreferences(0, 600, "prefRecentTurnout", sharedPreferences);
                removeExtraListDataFromPreferences(0, prefCount, "prefRecentTurnoutName", sharedPreferences);
                removeExtraListDataFromPreferences(0, prefCount, "prefRecentTurnoutSource", sharedPreferences);
                removeExtraListDataFromPreferences(0, prefCount, "prefRecentTurnoutServer", sharedPreferences);
            }
        }
        Log.d("Engine_Driver", "writeSharedPreferencesToFile: ImportExportPreferences: Saving preferences to file - Finished");
    }

    @SuppressLint({"ApplySharedPref", "StringFormatMatches"})
    public boolean loadSharedPreferencesFromFile(Context context, SharedPreferences sharedPreferences, String exportedPreferencesFileName, String deviceId, boolean clearRecentsIfNoFile) {
        Log.d("Engine_Driver", "loadSharedPreferencesFromFile: ImportExportPreferences: Loading saved preferences from file");
        currentlyImporting = true;
        boolean res = false;
        boolean srcExists = false;
        SharedPreferences.Editor prefEdit = sharedPreferences.edit();

        if (!exportedPreferencesFileName.equals(".ed")) {

            // save a few values so that we can reset them. i.e. effectively don't import them
            String currentThrottleNameValue = sharedPreferences.getString("throttle_name_preference", context.getResources().getString(R.string.prefThrottleNameDefaultValue)).trim();
            String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", context.getResources().getString(R.string.prefAutoImportExportDefaultValue));
            boolean prefForcedRestart = sharedPreferences.getBoolean("prefForcedRestart", false);
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
                    Log.d("Engine_Driver", "loadSharedPreferencesFromFile: Key Count:" + entries.size());
                    for (Map.Entry<String, ?> entry : entries.entrySet()) {
                        Object v = entry.getValue();
                        String key = entry.getKey();

//                        Log.d("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: Key Start: " + key);

                        if (v instanceof Boolean) {
//                            Log.d("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: Key End: " + key + " - boolean - " + v);
                            prefEdit.putBoolean(key, (Boolean) v);
                        } else if (v instanceof Float) {
//                            Log.d("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: Key End: " + key + " - Float - " + v);
                            prefEdit.putFloat(key, (Float) v);
                        } else if (v instanceof Integer) {
//                            Log.d("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: Key End: " + key + " - Integer - " + v);
                            prefEdit.putInt(key, (Integer) v);
                        } else if (v instanceof Long) {
//                            Log.d("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: Key End: " + key + " - Long - " + v);
                            prefEdit.putLong(key, (Long) v);
                        } else if (v instanceof String) {
//                            Log.d("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: Key End: " + key + " - String - " + v);
                            prefEdit.putString(key, ((String) v));
                            if (key.equals("prefAndroidId")) { restoredDeviceId = (String) v;}
                        }

                        Log.d("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: Key " + i +" End: " + key + " - " + v);
                        i++;
                    }
                    res = true;


                    // restore the remembered throttle name to avoid a duplicate throttle name if this is a different to device to where it was originally saved
                    if ((!restoredDeviceId.equals(deviceId)) || (restoredDeviceId.isEmpty())) {
                        prefEdit.putString("throttle_name_preference", currentThrottleNameValue);
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

                    Log.d("Engine_Driver", "ImportExportPreferences: " + m);
//                    Toast.makeText(context, m, Toast.LENGTH_SHORT).show();
                    threaded_application.safeToast(m, Toast.LENGTH_SHORT);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: " + e);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: " + e);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    Log.e("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: " + e);
                } finally {
                    try {
                        if (input != null) {
                            input.close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Log.e("Engine_Driver", "ImportExportPreferences: loadSharedPreferencesFromFile: " + ex);
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
                    writeRecentLocosListToFile(sharedPreferences);

                    getStringListDataFromPreferences(consistNameList, "prefRecentConsistName", sharedPreferences, -1, "");
                    for (int i = 0; i < consistNameList.size(); i++) {
                        ArrayList<Integer> tempConsistEngineAddressList_inner = new ArrayList<>();
                        ArrayList<Integer> tempConsistAddressSizeList_inner = new ArrayList<>();
                        ArrayList<Integer> tempConsistDirectionList_inner = new ArrayList<>();
                        ArrayList<Integer> tempConsistSourceList_inner = new ArrayList<>();
                        ArrayList<String> tempConsistRosterNameList_inner = new ArrayList<>();
                        ArrayList<Integer> tempConsistLightList_inner = new ArrayList<>();

                        getIntListDataFromPreferences(tempConsistEngineAddressList_inner, "prefRecentConsistAddress_"+i, sharedPreferences, -1, 0);
                        getIntListDataFromPreferences(tempConsistAddressSizeList_inner, "prefRecentConsistSize_"+i, sharedPreferences, tempConsistEngineAddressList_inner.size(), 0);
                        getIntListDataFromPreferences(tempConsistDirectionList_inner, "prefRecentConsistDirection_"+i, sharedPreferences, tempConsistEngineAddressList_inner.size(), 0);
                        getIntListDataFromPreferences(tempConsistSourceList_inner, "prefRecentConsistSource_"+i, sharedPreferences, tempConsistEngineAddressList_inner.size(), source_type.UNKNOWN);
                        getStringListDataFromPreferences(tempConsistRosterNameList_inner, "prefRecentConsistRosterName_"+i, sharedPreferences, tempConsistEngineAddressList_inner.size(), "");
                        getIntListDataFromPreferences(tempConsistLightList_inner, "prefRecentConsistLight_"+i, sharedPreferences, tempConsistEngineAddressList_inner.size(), light_follow_type.UNKNOWN);

                        consistEngineAddressList.add(tempConsistEngineAddressList_inner);
                        consistAddressSizeList.add(tempConsistAddressSizeList_inner);
                        consistDirectionList.add(tempConsistDirectionList_inner);
                        consistSourceList.add(tempConsistSourceList_inner);
                        consistRosterNameList.add(tempConsistRosterNameList_inner);
                        consistLightList.add(tempConsistLightList_inner);
                    }
                    writeRecentConsistsListToFile(sharedPreferences, -1);
                }
            } else {
                deleteFile(RECENT_ENGINES_FILENAME);
                deleteFile(RECENT_CONSISTS_FILENAME);
                deleteFile(RECENT_TURNOUTS_FILENAME);
            }

            if (!res) {
                if (srcExists) {
//                    Toast.makeText(context, context.getResources().getString(R.string.toastImportExportImportFailed, exportedPreferencesFileName), Toast.LENGTH_LONG).show();
                    threaded_application.safeToast(context.getResources().getString(R.string.toastImportExportImportFailed,
                                                        exportedPreferencesFileName), Toast.LENGTH_LONG);
                } else {
//                    Toast.makeText(context, context.getResources().getString(R.string.toastImportExportServerImportFailed, exportedPreferencesFileName), Toast.LENGTH_LONG).show();
                    threaded_application.safeToast(context.getResources().getString(R.string.toastImportExportServerImportFailed,
                                                        exportedPreferencesFileName), Toast.LENGTH_LONG);
                }
            }
        } else {
//            Toast.makeText(context, context.getResources().getString(R.string.toastImportExportCannotImport), Toast.LENGTH_LONG).show();
            threaded_application.safeToast(R.string.toastImportExportCannotImport, Toast.LENGTH_LONG);
        }

        prefEdit.commit();
        Log.d("Engine_Driver", "loadSharedPreferencesFromFile: ImportExportPreferences: Loading saved preferences from file - Finished");
        return res;
    }

    public void loadRecentLocosListFromFile() {
        Log.d("Engine_Driver", "loadRecentLocosListFromFile: ImportExportPreferences: Loading recent locos list from file");
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
                            recentLocoAddressList.add(addr);
                            recentLocoAddressSizeList.add(size);
                            String addressLengthString = ((size == 0) ? "S" : "L");  //show L or S based on length from file
                            String engineAddressString = String.format("%s(%s)", addr, addressLengthString);
                            if ((locoName.isEmpty() || locoName.equals(engineAddressString))) { // if nothing is stored, or what is stored is the same as the address, look for it in the roster
                                locoName = engineAddressString;
                            }
                            recentLocoNameList.add(locoName);
                            recentLocoSourceList.add(source);
                            recentLocoFunctionsList.add(functions);

                        }
                    }
                }
                list_reader.close();
            }
            Log.d("Engine_Driver", "loadRecentLocosListFromFile: ImportExportPreferences: Read recent locos list from file complete successfully");

        } catch (IOException except) {
            Log.e("Engine_Driver", "loadRecentLocosListFromFile: ImportExportPreferences: select_loco - Error reading recent loco file. "
                    + except.getMessage());
        }
    }

    public void writeRecentLocosListToFile(SharedPreferences sharedPreferences) {
        Log.d("Engine_Driver", "writeRecentLocosListToFile: ImportExportPreferences: Writing recent locos list to file");

        // write it out from the saved preferences to the file
        File engine_list_file = new File(context.getExternalFilesDir(null), RECENT_ENGINES_FILENAME);

        PrintWriter list_output;
        String smrl = sharedPreferences.getString("maximum_recent_locos_preference", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue)); //retrieve pref for max recent locos to show
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
            Log.d("Engine_Driver", "writeRecentLocosListToFile: ImportExportPreferences: Write recent locos list to file complete successfully");
        } catch (IOException except) {
            Log.e("Engine_Driver",
                    "writeRecentLocosListToFile caught IOException: "
                            + except.getMessage());
        } catch (NumberFormatException except) {
            Log.e("Engine_Driver",
                    "writeRecentLocosListToFile caught NumberFormatException: "
                            + except.getMessage());
        } catch (IndexOutOfBoundsException except) {
            Log.e("Engine_Driver",
                    "writeRecentLocosListToFile caught IndexOutOfBoundsException: "
                            + except.getMessage());
        }
    }

    public void loadRecentConsistsListFromFile() {
        Log.d("Engine_Driver", "loadRecentConsistsListFromFile: ImportExportPreferences: Loading recent consists list from file");

        consistEngineAddressList = new ArrayList<>();
        consistAddressSizeList = new ArrayList<>();
        consistDirectionList = new ArrayList<>();
        consistLightList = new ArrayList<>();
        consistSourceList = new ArrayList<>();
        consistRosterNameList = new ArrayList<>();
        consistNameList = new ArrayList<>();
        consistNameHtmlList = new ArrayList<>();

        ArrayList<Integer> tempConsistEngineAddressList_inner;
        ArrayList<Integer> tempConsistAddressSizeList_inner;
        ArrayList<Integer> tempConsistDirectionList_inner;
        ArrayList<Integer> tempConsistSourceList_inner;
        ArrayList<String> tempConsistRosterNameList_inner;
        ArrayList<Integer> tempConsistLightList_inner;

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
                        tempConsistEngineAddressList_inner = new ArrayList<>();
                        tempConsistAddressSizeList_inner = new ArrayList<>();
                        tempConsistDirectionList_inner = new ArrayList<>();
                        tempConsistSourceList_inner = new ArrayList<>();
                        tempConsistRosterNameList_inner = new ArrayList<>();
                        tempConsistLightList_inner = new ArrayList<>();

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
                                tempConsistRosterNameList_inner.addAll(Arrays.asList(rosterNames));
                            }
                        }

                        int splitLoco = line.indexOf(',');
                        if (splitLoco != -1) {
                            oneConsist.append(addOneConsistAddress(line, 0, splitLoco,
                                    tempConsistEngineAddressList_inner,
                                    tempConsistAddressSizeList_inner,
                                    tempConsistDirectionList_inner,
                                    tempConsistSourceList_inner,
                                    tempConsistLightList_inner));
                            oneConsistHtml.append(addOneConsistAddressHtml(
                                    tempConsistEngineAddressList_inner.get(0),
                                    tempConsistAddressSizeList_inner.get(0)
//                                    ,
//                                    tempConsistDirectionList_inner.get(0),
//                                    tempConsistSourceList_inner.get(0),
//                                    light_follow_type.FOLLOW)
                            ));

                            boolean foundOne = true;
                            while (foundOne) {
                                Integer prevSplitLoco = splitLoco + 1;
                                splitLoco = line.indexOf(',', prevSplitLoco);
                                if (splitLoco != -1) {
                                    oneConsist.append(addOneConsistAddress(line, prevSplitLoco, splitLoco,
                                            tempConsistEngineAddressList_inner,
                                            tempConsistAddressSizeList_inner,
                                            tempConsistDirectionList_inner,
                                            tempConsistSourceList_inner,
                                            tempConsistLightList_inner));
                                } else {
                                    oneConsist.append(addOneConsistAddress(line, prevSplitLoco, line.length(),
                                            tempConsistEngineAddressList_inner,
                                            tempConsistAddressSizeList_inner,
                                            tempConsistDirectionList_inner,
                                            tempConsistSourceList_inner,
                                            tempConsistLightList_inner));
                                    foundOne = false;
                                }
                                int lastItem = tempConsistEngineAddressList_inner.size()-1;
                                oneConsistHtml.append(addOneConsistAddressHtml(
                                        tempConsistEngineAddressList_inner.get(lastItem),
                                        tempConsistAddressSizeList_inner.get(lastItem)
//                                        ,
//                                        tempConsistDirectionList_inner.get(lastItem),
//                                        tempConsistSourceList_inner.get(lastItem),
//                                        tempConsistLightList_inner.get(lastItem)
                                ));
                            }
                            if (splitLine.length < 3) {  // old format - need to add some dummy roster names
                                for (int j = 0; j < tempConsistEngineAddressList_inner.size(); j++) {
                                    tempConsistRosterNameList_inner.add("");
                                }
                            }
                            consistEngineAddressList.add(tempConsistEngineAddressList_inner);
                            consistAddressSizeList.add(tempConsistAddressSizeList_inner);
                            consistDirectionList.add(tempConsistDirectionList_inner);
                            consistSourceList.add(tempConsistSourceList_inner);
                            consistRosterNameList.add(tempConsistRosterNameList_inner);
                            consistLightList.add(tempConsistLightList_inner);
                            if (consistName.isEmpty()) {
                                consistName = oneConsist.toString();
                            }
                            consistNameList.add(consistName);
                            consistNameHtmlList.add(oneConsistHtml.toString());
                        }
                    }
                    list_reader.close();
                    Log.d("Engine_Driver", "loadRecentConsistsListFromFile: ImportExportPreferences: Read recent consists list from file completed successfully");
                }

            } catch (IOException except) {
                Log.e("Engine_Driver", "loadRecentConsistsListFromFile: ImportExportPreferences: Error reading recent consist file. "
                        + except.getMessage());
            }
        }
    }

    private String addOneConsistAddress(String line, Integer start, Integer end,
                                        ArrayList<Integer> tempConsistEngineAddressList_inner,
                                        ArrayList<Integer> tempConsistAddressSizeList_inner,
                                        ArrayList<Integer> tempConsistDirectionList_inner,
                                        ArrayList<Integer> tempConsistSourceList_inner,
                                        ArrayList<Integer> tempConsistLightList_inner) {
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
            tempConsistEngineAddressList_inner.add(addr);
            tempConsistAddressSizeList_inner.add(size);
            tempConsistDirectionList_inner.add(dir);
            tempConsistSourceList_inner.add(source);
            tempConsistLightList_inner.add(light);

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

    public int addCurrentConsistToBeginningOfList(Consist consist) { // if necessary   return -1 if not currently in the list
        ArrayList<Integer> tempConsistEngineAddressList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistAddressSizeList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistDirectionList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistSourceList_inner = new ArrayList<>();
        ArrayList<String> tempConsistRosterNameList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistLightList_inner = new ArrayList<>();

        //if not updating list or no SD Card present then nothing else to do
        if ( (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            || (consist == null) )
            return -1;

        Collection<Consist.ConLoco> conLocos = consist.getLocos();


        for (Consist.ConLoco l : conLocos) {
            tempConsistEngineAddressList_inner.add(l.getIntAddress());
            tempConsistAddressSizeList_inner.add(l.getIntAddressLength());
            String addr = locoAddressToString(l.getIntAddress(), l.getIntAddressLength(), true);
            tempConsistDirectionList_inner.add((consist.isBackward(addr) ? 1 : 0));
            String rosterName = "";
            if (l.getRosterName() != null) {
                rosterName = l.getRosterName();
            }
//            tempConsistSourceList_inner.add(rosterName.equals("") ? source_type.ADDRESS : source_type.ROSTER);
            tempConsistSourceList_inner.add(l.getWhichSource());
            tempConsistRosterNameList_inner.add(rosterName);
            tempConsistLightList_inner.add((consist.isLight(addr)));
        }

        int whichEntryIsBeingUpdated = -1;
        // find out which entry it is in the roster
        boolean isSame = false;
        for (int i = 0; i < consistEngineAddressList.size() && !isSame; i++) {
            if (consistEngineAddressList.get(i).size() == tempConsistEngineAddressList_inner.size()) {  // if the lists are different sizes don't bother
                for (int j = 0; j < consistEngineAddressList.get(i).size() && !isSame; j++) {
                    if ((consistEngineAddressList.get(i).get(j).equals(tempConsistEngineAddressList_inner.get(j)))
//                            && (consistDirectionList.get(i).get(j).equals(tempConsistDirectionList_inner.get(j)))
                    ) {
                        isSame = true;
                    }
                }
                if (isSame) {
                    whichEntryIsBeingUpdated = i + 1; //remember this, so we can remove this line in the list.  Add 1 because we are gone to force a new line at the top
                }
            }
        }

        //add it to the beginning of the list
        consistEngineAddressList.add(0, tempConsistEngineAddressList_inner);
        consistAddressSizeList.add(0, tempConsistAddressSizeList_inner);
        consistDirectionList.add(0, tempConsistDirectionList_inner);
        consistSourceList.add(0, tempConsistSourceList_inner);
        consistRosterNameList.add(0, tempConsistRosterNameList_inner);
        consistLightList.add(0, tempConsistLightList_inner);

        String consistName = consist.toString();
        if (whichEntryIsBeingUpdated>0) { //this may already have a name
            consistName = consistNameList.get(whichEntryIsBeingUpdated-1);
        }
        consistNameList.add(0, consistName);

        return whichEntryIsBeingUpdated;
    }


    public void writeRecentConsistsListToFile(SharedPreferences sharedPreferences, int whichEntryIsBeingUpdated) {
        Log.d("Engine_Driver", "writeRecentConsistsListToFile: ImportExportPreferences: Writing recent consists list to file");

        // write it out from the saved preferences to the file
        File consist_list_file = new File(context.getExternalFilesDir(null), RECENT_CONSISTS_FILENAME);

        PrintWriter list_output;
        int numberOfRecentLocosToWrite = getIntPrefValue(sharedPreferences, "maximum_recent_locos_preference", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue));
        try {
            list_output = new PrintWriter(consist_list_file);
            if (numberOfRecentLocosToWrite > 0) {
                for (int i = 0; i < consistNameList.size() && numberOfRecentLocosToWrite > 0; i++) {

                    if (i!=whichEntryIsBeingUpdated) { // if this is the one being updated, don't write it

                        for (int j = 0; j < consistAddressSizeList.get(i).size(); j++) {
                            if (j > 0) {
                                list_output.format(",");
                            }
                            list_output.format("%d:%d%d%d%d",
                                    consistEngineAddressList.get(i).get(j),
                                    consistAddressSizeList.get(i).get(j),
                                    consistDirectionList.get(i).get(j),
                                    consistSourceList.get(i).get(j),
                                    (j==0 ? light_follow_type.FOLLOW :consistLightList.get(i).get(j)) );  // always set the first loco as 'follow'
                        }
                        list_output.format("<~>%s<~>", consistNameList.get(i));
                        for (int j = 0; j < consistRosterNameList.get(i).size(); j++) {
                            if (j > 0) {
                                list_output.format("<,>");
                            }
                            list_output.format("%s",
                                    consistRosterNameList.get(i).get(j));
                        }

                        list_output.format("\n");
                        numberOfRecentLocosToWrite--;
                    }
                }
            }
            list_output.flush();
            list_output.close();
            Log.d("Engine_Driver", "writeRecentConsistsListToFile: ImportExportPreferences: Write recent consists list to file completed successfully");
        } catch (IOException except) {
            Log.e("Engine_Driver",
                    "writeRecentConsistsListToFile: ImportExportPreferences: Error creating a PrintWriter, IOException: "
                            + except.getMessage());
        }
    }


    @SuppressLint("ApplySharedPref")
    private boolean saveIntListDataToPreferences(ArrayList<Integer> list, String listName, SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt(listName +"_size", list.size()).commit();
        int prefInt;
        for(int i=0 ; i<list.size() ; i++){
            prefInt=list.get(i);
            sharedPreferences.edit().putInt(listName + "_" + i, prefInt).commit();
        }
        return sharedPreferences.edit().commit();
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

//        Log.d("Engine_Driver", "writeRecentConsistsListToFile: removeExtraListDataFromPreferences: list: " +listName + " prefCount" + prefCount);

        return prefCount;
    }


    private int getIntListDataFromPreferences(ArrayList<Integer> list, String listName, SharedPreferences sharedPreferences, int forceSize, int defaultValue) {
        int size = sharedPreferences.getInt(listName + "_size", 0);
        if (forceSize>0) { // get a specified number regardless of how many are stored
            size = forceSize;
        }
        int prefInt;
        for(int i=0 ; i<size ; i++){
            prefInt = sharedPreferences.getInt(listName + "_" + i, defaultValue);
            list.add(prefInt);
        }
        return size;
    }

    @SuppressLint("ApplySharedPref")
    private boolean saveStringListDataToPreferences(ArrayList<String> list, String listName, SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt(listName +"_size", list.size()).commit();
        String prefString;
        for(int i=0 ; i<list.size() ; i++){
            prefString = list.get(i);
            sharedPreferences.edit().putString(listName + "_" + i, prefString).commit();
        }
        return sharedPreferences.edit().commit();
    }

    private int getStringListDataFromPreferences(ArrayList<String> list, String listName, SharedPreferences sharedPreferences, int forceSize, String defaultValue) {
        int size = sharedPreferences.getInt(listName + "_size", 0);
        if (forceSize>0) { // get a specified number regardless of how many are stored
            size = forceSize;
        }
        String prefString;
        for(int i=0 ; i<size ; i++){
            prefString = sharedPreferences.getString(listName + "_" + i, defaultValue);
            list.add(prefString);
        }
        return size;
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
            Log.e("Engine_Driver", "locoAddressToString. ");
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
            Log.e("Engine_Driver", "locoAddressToHtml. ");
        }
        return engineAddressHtml;
    }


//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public void writeRecentTurnoutsListToFile(SharedPreferences sharedPreferences) {
//        Log.d("Engine_Driver", "writeRecentTurnoutsListToFile: ImportExportPreferences: Writing recent turnouts list to file");

        File engine_list_file = new File(context.getExternalFilesDir(null), RECENT_TURNOUTS_FILENAME);

        PrintWriter list_output;
        String smrl = sharedPreferences.getString("maximum_recent_locos_preference", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue)); //retrieve pref for max recent locos to show
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
                deleteFile(RECENT_TURNOUTS_FILENAME);
                return;
            }
            list_output.flush();
            list_output.close();
            Log.d("Engine_Driver", "writeRecentTurnoutsListToFile: ImportExportPreferences: " +
                    "Write recent turnouts list to file completed successfully with " + recentTurnoutAddressList.size() + " entries.");
        } catch (IOException except) {
            Log.e("Engine_Driver",
                    "writeRecentTurnoutsListToFile: ImportExportPreferences: Error creating a PrintWriter, IOException: "
                            + except.getMessage());
        } catch (IndexOutOfBoundsException except) {
            Log.e("Engine_Driver",
                    "writeRecentTurnoutsListToFile: ImportExportPreferences: Error writing recent turnouts lists, IndexOutOfBoundsException: "
                            + except.getMessage());
        }
    }

    /** @noinspection UnusedReturnValue*/
    public boolean deleteFile(String filename) {
        Log.d("Engine_Driver", "deleteRecentTurnoutsListFile: ImportExportPreferences: delete file");

        File file = new File(context.getExternalFilesDir(null), filename);
        if (file.exists()) {
            try {
                file.delete();
                return(true);
            } catch (Exception except) {
                Log.e("Engine_Driver",
                        "deleteRecentTurnoutsListFile: ImportExportPreferences: Error deleting : " + filename
                                + except.getMessage());
            }
        }
        return(false);
    }

        public void loadRecentTurnoutsListFromFile() {
//        Log.d("Engine_Driver", "loadRecentTurnoutsListFromFile: ImportExportPreferences: Loading recent turnouts list from file");
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
                        Integer source = 0;
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
                        if (addr.length() > 0) {
                            recentTurnoutAddressList.add(addr);
                            recentTurnoutNameList.add(turnoutName);
                            recentTurnoutSourceList.add(source);
                            recentTurnoutServerList.add(turnoutServer);

                        }
                    }
                }
                list_reader.close();
            }
            Log.d("Engine_Driver", "loadRecentTurnoutsListFromFile: ImportExportPreferences: " +
                    "Read recent turnouts list from file completed successfully with " + recentTurnoutAddressList.size() + " entries.");

        } catch (IOException except) {
            Log.e("Engine_Driver", "loadRecentTurnoutsListFromFile: ImportExportPreferences: Error reading recent turnouts file. "
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
        loadRecentLocosListFromFile();

        ArrayList<String> rns = new ArrayList<>(mainapp.roster_entries.keySet());  //copy from synchronized map to avoid holding it while iterating
        int recentsSize = getIntPrefValue(sharedPreferences, "maximum_recent_locos_preference", context.getResources().getString(R.string.prefMaximumRecentLocosDefaultValue));
        int requiredRecentsSize = rns.size()+ 10;
        if (requiredRecentsSize>recentsSize) { // force the preference for the max recents to larger than the roster
            sharedPreferences.edit().putString("maximum_recent_locos_preference", String.format("%d",requiredRecentsSize)).commit();  //reset the preference
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
            if (ras[0].length() > 0) {  //only process if address found
                locoAddressSize = (ras[1].charAt(0) == 'L') ? address_type.LONG : address_type.SHORT;   // convert S/L to 0/1
                try {
                    locoAddress = Integer.parseInt(ras[0]);   // convert address to int
                } catch (NumberFormatException e) {
//                    Toast.makeText(context, "ERROR - could not parse address\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    threaded_application.safeToast(context.getResources().getString(R.string.toastImportExportCouldNotParseAddress, e.getMessage()), Toast.LENGTH_SHORT);
                    return; //get out, don't try to acquire
                }
                if ("loco".equals(rosterEntryType)) {
                    locoName = rosterNameString;
                }
            }

            // check if it is already in the list and remove it
            String keepFunctions = "";
            for (int i = 0; i < recentLocoAddressList.size(); i++) {
                Log.d("Engine_Driver", "importExportPreferences: downloadRosterToRecents: locoName='"+locoName+"', address=" + locoAddress);
                if (locoAddress == recentLocoAddressList.get(i)
                        && locoAddressSize == recentLocoAddressSizeList.get(i)
                        && locoName.equals(recentLocoNameList.get(i))) {

                    recentLocoAddressList.remove(i);
                    recentLocoAddressSizeList.remove(i);
                    recentLocoNameList.remove(i);
                    recentLocoSourceList.remove(i);
                    keepFunctions = recentLocoFunctionsList.get(i);
                    recentLocoFunctionsList.remove(i);
                    Log.d("Engine_Driver", "importExportPreferences: downloadRosterToRecents: Loco '"+ locoName + "' removed from Recents");
                    break;
                }
            }

            // now append it to the end of the list
            int endOfList = recentLocoAddressList.size();
            recentLocoAddressList.add(endOfList, locoAddress);
            recentLocoAddressSizeList.add(endOfList, locoAddressSize);
            recentLocoNameList.add(endOfList, locoName);
            recentLocoSourceList.add(endOfList, source_type.ROSTER);
            recentLocoFunctionsList.add(endOfList, keepFunctions);  // blank for now

            Log.d("Engine_Driver", "importExportPreferences: downloadRosterToRecents: Loco '"+ locoName + "' added to Recents");

            j++;
        }

        writeRecentLocosListToFile(sharedPreferences);

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
