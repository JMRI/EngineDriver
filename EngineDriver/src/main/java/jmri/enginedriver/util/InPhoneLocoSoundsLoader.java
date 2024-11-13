package jmri.enginedriver.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import jmri.enginedriver.R;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.sounds_type;

public class InPhoneLocoSoundsLoader {
   protected threaded_application mainapp;  // hold pointer to mainapp
   protected SharedPreferences prefs;
   protected Context context;

   // details for the currently being loaded IPLS file
   // these are used for temporary storage of the ipls details
   public String[] iplsLocoSoundsFileName = {"","","","","", "","","","","", "","","","","", "","","","","", "",""};  // idle, 1-16   20 ans 21 are startup and shut down
   public String[] iplsBellSoundsFileName = {"","",""};  // Start, Loop, End
   public String[] iplsHornSoundsFileName = {"","",""};  // Start, Loop, End
   public String iplsHornShortSoundsFileName = "";
   public int iplsLocoSoundsCount = -1;
   public String iplsName = "";   // name for the menus
   public String iplsFileName = "";

   private int soundsCountOfSoundBeingLoaded = 0;

   /** @noinspection EmptyMethod*/
   protected void onPreExecute() {
   }

   public InPhoneLocoSoundsLoader(threaded_application myApp, SharedPreferences myPrefs, Context myContext) {
      mainapp = myApp;
      prefs = myPrefs;
      context = myContext;
   }

   public class LoadSoundCompleteDelayed implements Runnable {
      int loadDelay;

      public LoadSoundCompleteDelayed(int delay) {
         loadDelay = delay;
      }

      @Override
      public void run() {
         Log.d("Engine_Driver", "LoadSoundCompleteDelayed.run: (locoSound)");
         mainapp.soundsSoundsAreBeingReloaded = false;
         mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.SOUNDS_FORCE_LOCO_SOUNDS_TO_START, "", 0);
//            Toast.makeText(getApplicationContext(), "Sounds loaded. Delay: " + loadDelay, Toast.LENGTH_SHORT).show();
         Log.d("Engine_Driver", "LoadSoundCompleteDelayed.run. Delay: " + loadDelay);

      }
   } // end DoLocoSoundDelayed


   @SuppressLint({"ApplySharedPref", "DiscouragedApi"})
   public boolean loadSounds() {
      Log.d("Engine_Driver", "loadSounds: (locoSound)");
      mainapp.prefDeviceSounds[0] = prefs.getString("prefDeviceSounds0", context.getResources().getString(R.string.prefDeviceSoundsDefaultValue));
      mainapp.prefDeviceSounds[1] = prefs.getString("prefDeviceSounds1", context.getResources().getString(R.string.prefDeviceSoundsDefaultValue));

      boolean soundAlreadyLoaded = true;
      for (int throttleIndex = 0; throttleIndex < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES; throttleIndex++) {
         if (!mainapp.prefDeviceSoundsCurrentlyLoaded[throttleIndex].equals(mainapp.prefDeviceSounds[throttleIndex])) {
            soundAlreadyLoaded = false;
            break;
         }
      }
      if (soundAlreadyLoaded) {
         mainapp.soundsReloadSounds = false;
         return false;
      }

      mainapp.soundsSoundsAreBeingReloaded = true;
//        Log.d("Engine_Driver", "loadSounds: (locoSound): sounds really do need to be reloaded");

      if (mainapp.soundPool!=null) {
         mainapp.stopAllSounds();
      }
      for (int i = 0; i <= 1; i++) {
         mainapp.soundsLocoQueue[i] = new ArrayQueue(30);
         mainapp.soundsLocoQueue[i].emptyQueue();
         mainapp.soundsLocoCurrentlyPlaying[i] = -1;
      }

      // setup the soundPool for the in device loco sounds
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
         AudioAttributes audioAttributes = new AudioAttributes
                 .Builder()
                 .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                 .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                 .build();
         mainapp.soundPool = new SoundPool
                 .Builder()
                 .setMaxStreams(8)
                 .setAudioAttributes(audioAttributes)
                 .build();

      } else {
         mainapp.soundPool = new SoundPool(8,
                 AudioManager.STREAM_MUSIC,0);
      }
      mainapp.soundPool.setOnLoadCompleteListener (new SoundPool.OnLoadCompleteListener () {
         @Override
         public void onLoadComplete(SoundPool soundPool, int i, int i2) {
            if (i==soundsCountOfSoundBeingLoaded) {
               Log.d("Engine_Driver", "loadSounds: Sounds confirmed loaded.");
               if (mainapp.throttle_msg_handler!=null) {
                  mainapp.throttle_msg_handler.postDelayed(
                          new LoadSoundCompleteDelayed(1000), 1000);
               }
            }
         }
      });

      clearAllSounds();

      for (int throttleIndex = 0; throttleIndex <= 1; throttleIndex++) {
         if (mainapp.prefDeviceSounds[throttleIndex].toLowerCase(Locale.ROOT).contains(".ipls")) {
            // load the custom sounds
            getIplsDetails(mainapp.prefDeviceSounds[throttleIndex]);
            if (!iplsFileName.equals("")) {
               for (int j = 0; j <= 2; j++) {
                  loadSoundFromFile(sounds_type.BELL, throttleIndex, j, context, iplsBellSoundsFileName[j]);
                  loadSoundFromFile(sounds_type.HORN, throttleIndex, j, context, iplsHornSoundsFileName[j]);
               }
               loadSoundFromFile(sounds_type.HORN_SHORT, throttleIndex, 0, context, iplsHornShortSoundsFileName);
               for (int j = 0; j <= iplsLocoSoundsCount; j++) {
                  loadSoundFromFile(sounds_type.LOCO, throttleIndex, j, context, iplsLocoSoundsFileName[j]);
               }
               loadSoundFromFile(sounds_type.LOCO, throttleIndex, sounds_type.STARTUP_INDEX, context, iplsLocoSoundsFileName[sounds_type.STARTUP_INDEX]);
               loadSoundFromFile(sounds_type.LOCO, throttleIndex, sounds_type.SHUTDOWN_INDEX, context, iplsLocoSoundsFileName[sounds_type.SHUTDOWN_INDEX]);
               mainapp.prefDeviceSoundsCurrentlyLoaded[throttleIndex] = iplsFileName;

            } else { // can't find the file name or some other issue
               mainapp.prefDeviceSounds[throttleIndex] = "none";
//               prefs.edit().putString("prefDeviceSoundsBellVolume", "100").commit();
               mainapp.prefDeviceSoundsCurrentlyLoaded[throttleIndex] = "none";
            }
            mainapp.soundsLocoSteps[throttleIndex] = iplsLocoSoundsCount;

         } else {  // use included sounds

            if (mainapp.prefDeviceSounds[throttleIndex].equals("none")) {
               mainapp.prefDeviceSounds[throttleIndex] = "none";
//               prefs.edit().putString("prefDeviceSoundsBellVolume", "100").commit();
               mainapp.prefDeviceSoundsCurrentlyLoaded[throttleIndex] = "none";

            } else {
               int sound;
               @SuppressLint("DiscouragedApi") int locoSoundsId = mainapp.getApplicationContext().getResources().getIdentifier("locoSounds_" + mainapp.prefDeviceSounds[throttleIndex], "array", context.getApplicationInfo().packageName);
               @SuppressLint("DiscouragedApi") int locoSoundsHornId = mainapp.getApplicationContext().getResources().getIdentifier("locoSoundsHorn_" + mainapp.prefDeviceSounds[throttleIndex], "array", context.getApplicationInfo().packageName);
               @SuppressLint("DiscouragedApi") int locoSoundsHornShortId = mainapp.getApplicationContext().getResources().getIdentifier("locoSoundsHornShort_" + mainapp.prefDeviceSounds[throttleIndex], "array", context.getApplicationInfo().packageName);
               @SuppressLint("DiscouragedApi") int locoSoundsBellId = mainapp.getApplicationContext().getResources().getIdentifier("locoSoundsBell_" + mainapp.prefDeviceSounds[throttleIndex], "array", context.getApplicationInfo().packageName);

               mainapp.prefDeviceSoundsCurrentlyLoaded[throttleIndex] = mainapp.prefDeviceSounds[throttleIndex];

               String[] locoSounds = mainapp.getApplicationContext().getResources().getStringArray(locoSoundsId);
               mainapp.soundsLocoSteps[throttleIndex] = 0;
               for (int i = 0; i < locoSounds.length; i++) {
                  if (!locoSounds[i].isEmpty()) {
                     sound = mainapp.getApplicationContext().getResources().getIdentifier(locoSounds[i], "raw", context.getApplicationInfo().packageName);
                     loadSound(sounds_type.LOCO, throttleIndex, i, context, sound);
                     if (i < sounds_type.STARTUP_INDEX) mainapp.soundsLocoSteps[throttleIndex] = i;
                  }
               }

               String[] locoSoundsHorn = mainapp.getApplicationContext().getResources().getStringArray(locoSoundsHornId);
               for (int i = 0; i < locoSoundsHorn.length; i++) {
                  sound = mainapp.getApplicationContext().getResources().getIdentifier(locoSoundsHorn[i], "raw", context.getApplicationInfo().packageName);
                  loadSound(sounds_type.HORN, throttleIndex, i, context, sound);
               }

               String[] locoSoundsHornShort = mainapp.getApplicationContext().getResources().getStringArray(locoSoundsHornShortId);
               for (int i = 0; i < locoSoundsHornShort.length; i++) {
                  sound = mainapp.getApplicationContext().getResources().getIdentifier(locoSoundsHornShort[i], "raw", context.getApplicationInfo().packageName);
                  loadSound(sounds_type.HORN_SHORT, throttleIndex, i, context, sound);
               }

               String[] locoSoundsBell = mainapp.getApplicationContext().getResources().getStringArray(locoSoundsBellId);
               for (int i = 0; i < locoSoundsBell.length; i++) {
                  sound = mainapp.getApplicationContext().getResources().getIdentifier(locoSoundsBell[i], "raw", context.getApplicationInfo().packageName);
                  loadSound(sounds_type.BELL, throttleIndex, i, context, sound);
               }
            }
         }
      }
      mainapp.soundsReloadSounds = false;
//      mainapp.throttle_msg_handler.postDelayed(
//              new LoadSoundCompleteDelayed(3000), 3000);

      boolean soundsLoading = false;
      for (int i = 0; i < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES; i++) {
         if (!mainapp.prefDeviceSounds[i].equals("none")) {
            soundsLoading = true;
            break;
         }
      }
      if (soundsLoading) {
//         Toast.makeText(context, R.string.toastInitialisingSounds, Toast.LENGTH_LONG).show();
         threaded_application.safeToast(R.string.toastInitialisingSounds, Toast.LENGTH_LONG);
      }
      return true;   // true = sounds were reloaded
   } // end loadSounds()

   void loadSound(int soundType, int whichThrottle, int soundNo, Context context, int resId) {
      int duration = 0;
      MediaPlayer player = MediaPlayer.create(context, resId);
      if (player!=null)
         duration = player.getDuration();
      soundsCountOfSoundBeingLoaded ++;
      switch (soundType) {
         default:
         case sounds_type.LOCO: // loco
            mainapp.soundsLoco[whichThrottle][soundNo] = mainapp.soundPool.load(context, resId, 1);
            mainapp.soundsLocoDuration[whichThrottle][soundNo] = duration;
            break;
         case sounds_type.BELL: // bell
         case sounds_type.HORN: // horn
         case sounds_type.HORN_SHORT: // horn short
            mainapp.soundsExtras[soundType-1][whichThrottle][soundNo] = mainapp.soundPool.load(context, resId, 1);
            mainapp.soundsExtrasDuration[soundType-1][whichThrottle][soundNo] = duration;
            break;
      }
   } // end loadSound()

   void loadSoundFromFile(int soundType, int whichThrottle, int soundNo, Context context, String fileName) {
//        Log.d("Engine_Driver", "loadSoundFromFile (locoSound): file: '" + fileName + "' wt: " + whichThrottle + " sNo: " + soundNo);
      int duration;

      if (fileName.length() > 0) {
         File file = new File(context.getExternalFilesDir(null), fileName);

         if(!file.exists()) {
            Log.d("Engine_Driver", "loadSoundFromFile (locoSound): file:'" + file.getPath() + "/" + fileName + "' - File can't be found");
            loadSoundFromFileFailed(soundType, whichThrottle, soundNo, context, fileName);
         } else {

            MediaPlayer player = MediaPlayer.create(context, Uri.fromFile(file));
            if (player == null) {
               Log.d("Engine_Driver", "loadSoundFromFile (locoSound): file:'" + file.getPath() + "/" + fileName + "' - Can't determine duration");
               loadSoundFromFileFailed(soundType, whichThrottle, soundNo, context, fileName);
            } else {
               duration = player.getDuration();
               soundsCountOfSoundBeingLoaded++;

               switch (soundType) {
                  default:
                  case sounds_type.LOCO: // loco
                     mainapp.soundsLoco[whichThrottle][soundNo]
                             = mainapp.soundPool.load(context.getExternalFilesDir(null) + "/" + fileName, 1);
                     mainapp.soundsLocoDuration[whichThrottle][soundNo] = duration;
                     break;
                  case sounds_type.BELL: // bell
                  case sounds_type.HORN: // horn
                  case sounds_type.HORN_SHORT: // horn short
                     mainapp.soundsExtras[soundType - 1][whichThrottle][soundNo]
                             = mainapp.soundPool.load(context.getExternalFilesDir(null) + "/" + fileName, 1);
                     mainapp.soundsExtrasDuration[soundType - 1][whichThrottle][soundNo] = duration;
                     break;
               }
               Log.d("Engine_Driver", "loadSoundFromFile (locoSound) : file loaded: '" + file.getPath() + "/" + fileName + "' wt: " + whichThrottle + " sNo: " + soundNo + " Duration: " + duration);
            }
         }
      } else {
         loadSoundFromFileFailed(soundType, whichThrottle, soundNo, context, fileName);
      }
   } // end loadSoundsFromFile()

   void loadSoundFromFileFailed(int soundType, int whichThrottle, int soundNo, Context context, String fileName) {
      switch (soundType) {
         case sounds_type.BELL: // bell
         case sounds_type.HORN: // horn
         case sounds_type.HORN_SHORT: // horn
            mainapp.soundsExtras[soundType-1][whichThrottle][soundNo] = 0;
            mainapp.soundsExtrasDuration[soundType-1][whichThrottle][soundNo] = 0;
            break;
         case sounds_type.LOCO: // loco
         default:
            mainapp.soundsLoco[whichThrottle][soundNo] = 0;
            mainapp.soundsLocoDuration[whichThrottle][soundNo] = 0;
            break;
      }
   }

   public void getIplsList() { // In Phone Loco Sounds
        mainapp.iplsFileNames = new ArrayList<>();
        mainapp.iplsNames = new ArrayList<>();

        File dir = new File(context.getExternalFilesDir(null).getPath());
        File [] filesList = dir.listFiles();
        for (File file : filesList) {
            String fileName = file.getName();
            String lowercaseFileName = file.getName().toLowerCase();
            if (lowercaseFileName.endsWith(".ipls")) {
                getIplsDetails(fileName);
                if (!iplsName.equals("")) { // if we didn't fine a name, ignore it
                   mainapp.iplsFileNames.add(fileName);
                   mainapp.iplsNames.add("♫  " + iplsName);
                }

                Log.d("Engine_Driver", "getIplsList: Found: " + fileName);
            }
        }
    }

   public void getIplsDetails(String fileName) {
      String name = "";
      String cmd;
      int num;
      iplsLocoSoundsCount =-1;

      File iplsFile = new File(context.getExternalFilesDir(null), fileName);
      if (iplsFile.exists()) {
         BufferedReader list_reader;
         try {
            list_reader = new BufferedReader(
                    new FileReader(iplsFile));
            while (list_reader.ready()) {
               String line = list_reader.readLine();
               if (line!=null) {
                  int splitPos = line.indexOf(':');
                  if (splitPos > 0) {
                     cmd = line.substring(0, 1).toLowerCase();
                     num = -1;
                     switch (cmd) {
                        case "/": // comment line
                           break;
                        case "n":
                           if (line.length() > splitPos + 1) { // has the name
                              name = line.substring(splitPos + 1, line.length() - splitPos + 1).trim();
                           }
                           break;
                        case "l":
                           if (splitPos > 1) {
                              try {
                                 num = Integer.decode(line.substring(1, splitPos));
                              } catch (NumberFormatException e) {
                                 if (line.substring(1, splitPos).equals("+")) {  // startup sound
                                    num = sounds_type.STARTUP_INDEX;
                                 } else if (line.substring(1, splitPos).equals("-")) { // shutdown sound
                                    num = sounds_type.SHUTDOWN_INDEX;
                                 } else {
                                    break;
                                 }
                              }
                           }
                           if ((num >= 0) && (num <= sounds_type.SHUTDOWN_INDEX)) {
                              iplsLocoSoundsFileName[num] = line.substring(splitPos + 1, line.length() - splitPos + 2).trim();
                              if ( (num > iplsLocoSoundsCount) && (num < sounds_type.STARTUP_INDEX) ) {  // don't count the Startup and shutdown sounds
                                 iplsLocoSoundsCount = num;
                              }
                           }
                           break;
                        case "b":
                           if (splitPos > 1) {
                              try {
                                 num = Integer.decode(line.substring(1, splitPos));
                              } catch (NumberFormatException e) {
                                 if (line.substring(1, splitPos).equals("+")) {  // startup sound
                                    num = sounds_type.STARTUP_INDEX;
                                 } else if (line.substring(1, splitPos).equals("-")) { // shutdown sound
                                    num = sounds_type.SHUTDOWN_INDEX;
                                 } else {
                                    break;
                                 }
                              }
                           }
                           if ((num >= 0) && (num <= 2)) {
                              iplsBellSoundsFileName[num] = line.substring(splitPos + 1, line.length() - splitPos + 2).trim();
                           }
                           break;
                        case "h":
                           if (splitPos > 1) {
                              try {
                                 num = Integer.decode(line.substring(1, splitPos));
                              } catch (NumberFormatException e) {
                                 if (line.substring(1, splitPos).equals("+")) {  // startup sound
                                    iplsHornShortSoundsFileName = line.substring(splitPos + 1, line.length() - splitPos + 2).trim();
                                 }
                                 break;
                              }
                           }
                           if ((num >= 0) && (num <= 2)) {
                              iplsHornSoundsFileName[num] = line.substring(splitPos + 1, line.length() - splitPos + 2).trim();
                           }
                           break;

                     }
                  }
               }
            }
            list_reader.close();

            iplsFileName = fileName;
            iplsName = name;

         } catch (IOException except) {
            Log.e("Engine_Driver", "addToIplsList: Error reading .ipls file. "
                    + except.getMessage());
         }

      }
      Log.d("Engine_Driver", "loadRecentLocosListFromFile: ImportExportPreferences: Read recent locos list from file complete successfully");
   }

   public void clearAllSounds() {
      Log.d("Engine_Driver", "iplsLoader - clearAllSounds (locoSounds)");
      for (int soundType = 0; soundType < 3; soundType++) {
         for (int throttleIndex = 0; throttleIndex < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES; throttleIndex++) {
            for (int mSound = 0; mSound < mainapp.soundsExtrasStreamId[soundType][throttleIndex].length; mSound++) {
               mainapp.soundsExtras[soundType][throttleIndex][mSound] = 0;
               mainapp.soundsExtrasStreamId[soundType][throttleIndex][mSound] = 0;
               mainapp.soundsExtrasDuration[soundType][throttleIndex][mSound] = 0;
            }
         }
      }
      for (int throttleIndex = 0; throttleIndex < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES; throttleIndex++) {
         mainapp.soundsLocoSteps[throttleIndex] = 0;
         for (int mSound = 0; mSound < mainapp.soundsLocoStreamId[throttleIndex].length; mSound++) {
            mainapp.soundsLoco[throttleIndex][mSound] = 0;
            mainapp.soundsLocoStreamId[throttleIndex][mSound] = 0;
            mainapp.soundsLocoDuration[throttleIndex][mSound] = 0;
         }
      }

      Arrays.fill(iplsLocoSoundsFileName, "");

      for (int i = 0; i < 3; i++) {
         iplsBellSoundsFileName[i] = "";
         iplsHornSoundsFileName[i] = "";
      }
      iplsHornShortSoundsFileName = "";
      iplsLocoSoundsCount = -1;
      iplsName = "";   // name for the menus
      iplsFileName = "";

   } // end clearAllSounds

}
