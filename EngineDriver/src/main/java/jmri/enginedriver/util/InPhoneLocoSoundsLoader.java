package jmri.enginedriver.util;

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
import java.util.Locale;

import jmri.enginedriver.R;
import jmri.enginedriver.message_type;
import jmri.enginedriver.threaded_application;

public class InPhoneLocoSoundsLoader {
   protected threaded_application mainapp;  // hold pointer to mainapp
   protected SharedPreferences prefs;
   protected Context context;

   private static final int SOUNDS_TYPE_LOCO = 0;
   private static final int SOUNDS_TYPE_BELL = 1;
   private static final int SOUNDS_TYPE_HORN = 2;
   private static final int SOUNDS_TYPE_HORN_SHORT = 3;

   private static final int SOUNDS_BELL_HORN_START = 0;
   private static final int SOUNDS_BELL_HORN_LOOP = 1;
   private static final int SOUNDS_BELL_HORN_END = 2;

   private static final int SOUNDS_STARTUP_INDEX = 20;
   private static final int SOUNDS_SHUTDOWN_INDEX = 21;

   // details for the currently being loaded IPLS file
   // these are used for temporary storage of the ipls details
   public String[] iplsLocoSoundsFileName = {"","","","","", "","","","","", "","","","","", "","","","","", "",""};  // idle, 1-16   20 ans 21 are startup and shut down
   public String[] iplsBellSoundsFileName = {"","",""};  // Start, Loop, End
   public String[] iplsHornSoundsFileName = {"","",""};  // Start, Loop, End
   public String iplsHornShortSoundsFileName = "";
   public int iplsLocoSoundsCount = -1;
   public String iplsName = "";   // name for the menues
   public String iplsFileName = "";

   private int soundsCountOfSoundBeingLoaded = 0;

   protected void onPreExecute() {
   }

   public InPhoneLocoSoundsLoader(threaded_application myApp, SharedPreferences myPrefs, Context myContext) {
      mainapp = myApp;
      prefs = myPrefs;
      context = myContext;
   }

   public class LoadSoundCompleteDelayed implements Runnable {
      int loadDelay =0;
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


   public boolean loadSounds() {
      Log.d("Engine_Driver", "loadSounds: (locoSound)");
      mainapp.prefDeviceSounds[0] = prefs.getString("prefDeviceSounds0", context.getResources().getString(R.string.prefDeviceSoundsDefaultValue));
      mainapp.prefDeviceSounds[1] = prefs.getString("prefDeviceSounds1", context.getResources().getString(R.string.prefDeviceSoundsDefaultValue));

      boolean soundAlreadyLoaded = true;
      for (int throttleIndex = 0; throttleIndex < mainapp.SOUND_MAX_SUPPORTED_THROTTLES; throttleIndex++) {
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
               Log.d("Engine_Driver", "loadSounds: Sounds confimed loaded.");
               mainapp.throttle_msg_handler.postDelayed(
                       new LoadSoundCompleteDelayed(1000), 1000);

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
                  loadSoundFromFile(SOUNDS_TYPE_BELL, throttleIndex, j, context, iplsBellSoundsFileName[j]);
                  loadSoundFromFile(SOUNDS_TYPE_HORN, throttleIndex, j, context, iplsHornSoundsFileName[j]);
               }
               loadSoundFromFile(SOUNDS_TYPE_HORN_SHORT, throttleIndex, 0, context, iplsHornShortSoundsFileName);
               for (int j = 0; j <= iplsLocoSoundsCount; j++) {
                  loadSoundFromFile(SOUNDS_TYPE_LOCO, throttleIndex, j, context, iplsLocoSoundsFileName[j]);
               }
               loadSoundFromFile(SOUNDS_TYPE_LOCO, throttleIndex, SOUNDS_STARTUP_INDEX, context, iplsLocoSoundsFileName[SOUNDS_STARTUP_INDEX]);
               loadSoundFromFile(SOUNDS_TYPE_LOCO, throttleIndex, SOUNDS_SHUTDOWN_INDEX, context, iplsLocoSoundsFileName[SOUNDS_SHUTDOWN_INDEX]);
               mainapp.prefDeviceSoundsCurrentlyLoaded[throttleIndex] = iplsFileName;

            } else { // can't find the file name or some other issue
               mainapp.prefDeviceSounds[throttleIndex] = "none";
               prefs.edit().putString("prefDeviceSoundsBellVolume", "none").commit();
               mainapp.prefDeviceSoundsCurrentlyLoaded[throttleIndex] = "none";
            }
            mainapp.soundsLocoSteps[throttleIndex] = iplsLocoSoundsCount;

         } else {
            switch (mainapp.prefDeviceSounds[throttleIndex]) {
               default:
               case "steam":
               case "steamSlow":
               case "steamClass64":
               case "diesel645turbo":
               case "diesel7FDL":
               case "dieselNW2":
                  loadSound(SOUNDS_TYPE_BELL, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.bell_start);
                  loadSound(SOUNDS_TYPE_BELL, throttleIndex, SOUNDS_BELL_HORN_LOOP, context, R.raw.bell_loop);
                  loadSound(SOUNDS_TYPE_BELL, throttleIndex, SOUNDS_BELL_HORN_END, context, R.raw.bell_end);
                  break;
               case "steamClass94":
                  loadSound(SOUNDS_TYPE_BELL, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.bell_br_64_glocke_22_start);
                  loadSound(SOUNDS_TYPE_BELL, throttleIndex, SOUNDS_BELL_HORN_LOOP, context, R.raw.bell_br_64_glocke_22_loop);
                  loadSound(SOUNDS_TYPE_BELL, throttleIndex, SOUNDS_BELL_HORN_END, context, R.raw.bell_br_64_glocke_22_end);
                  break;
            }

            switch (mainapp.prefDeviceSounds[throttleIndex]) {
               default:
               case "steam":
               case "steamSlow":
//                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.whistle_start);
//                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_LOOP, context, R.raw.whistle_loop);
//                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_END, context, R.raw.whistle_end);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.steam_horn_start3);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_LOOP, context, R.raw.steam_horn_loop3);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_END, context, R.raw.steam_horn_end3);
                  break;

               case "diesel645turbo":
               case "diesel7FDL":
               case "dieselNW2":
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.horn_start);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_LOOP, context, R.raw.horn_loop);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_END, context, R.raw.horn_end);
                  break;

               case "steamClass64":
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.whistle_class64_long_start);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_LOOP, context, R.raw.whistle_class64_long_mid);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_END, context, R.raw.whistle_class64_long_end);
                  break;

               case "steamClass94":
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.whistle_class94_pfiff_941538_b_nf_2_22_start);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_LOOP, context, R.raw.whistle_class94_pfiff_941538_b_nf_2_22_loop);
                  loadSound(SOUNDS_TYPE_HORN, throttleIndex, SOUNDS_BELL_HORN_END, context, R.raw.whistle_class94_pfiff_941538_b_nf_2_22_end);
                  break;
            }

            switch (mainapp.prefDeviceSounds[throttleIndex]) {
               default:
               case "steam":
               case "steamSlow":
                  loadSound(SOUNDS_TYPE_HORN_SHORT, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.whistle_short);
                  break;

               case "diesel645turbo":
               case "diesel7FDL":
               case "dieselNW2":
                  loadSound(SOUNDS_TYPE_HORN_SHORT, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.horn_short);
                  break;

               case "steamClass64":
                  loadSound(SOUNDS_TYPE_HORN_SHORT, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.whistle_class64_short);
                  break;

               case "steamClass94":
                  loadSound(SOUNDS_TYPE_HORN_SHORT, throttleIndex, SOUNDS_BELL_HORN_START, context, R.raw.whistle_class94_pfiff_2_2);
                  break;
            }

            mainapp.prefDeviceSoundsCurrentlyLoaded[throttleIndex] = mainapp.prefDeviceSounds[throttleIndex];
            switch (mainapp.prefDeviceSounds[throttleIndex]) {
               default:
               case "steam":
               case "steamSlow":
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 0, context, R.raw.steam_loco_stationary_med);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 1, context, R.raw.steam_piston_stroke3);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 2, context, R.raw.steam_loop_30rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 3, context, R.raw.steam_loop_35rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 4, context, R.raw.steam_loop_40rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 5, context, R.raw.steam_loop_50rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 6, context, R.raw.steam_loop_60rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 7, context, R.raw.steam_loop_75rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 8, context, R.raw.steam_loop_90rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 9, context, R.raw.steam_loop_100rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 10, context, R.raw.steam_loop_125rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 11, context, R.raw.steam_loop_150rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 12, context, R.raw.steam_loop_175rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 13, context, R.raw.steam_loop_200rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 14, context, R.raw.steam_loop_250rpm);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 15, context, R.raw.steam_loop_300rpm);
                  if (mainapp.prefDeviceSounds[throttleIndex].equals("steam")) {
                     mainapp.soundsLocoSteps[throttleIndex] = 15;
                  } else {
                     mainapp.soundsLocoSteps[throttleIndex] = 7;
                  }
                  break;

               case "diesel645turbo":
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 0, context, R.raw.diesel_645turbo_idle);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 1, context, R.raw.diesel_645turbo_d1k);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 2, context, R.raw.diesel_645turbo_d2);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 3, context, R.raw.diesel_645turbo_d3);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 4, context, R.raw.diesel_645turbo_d4);
                  mainapp.soundsLocoSteps[throttleIndex] = 4;
                  break;

               case "diesel7FDL":
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 0, context, R.raw.diesel_7fdl_idle_1a);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 1, context, R.raw.diesel_7fdl_idle_2a);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 2, context, R.raw.diesel_7fdl_idle_3a);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 3, context, R.raw.diesel_7fdl_idle_4a);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 4, context, R.raw.diesel_7fdl_idle_5a);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 5, context, R.raw.diesel_7fdl_idle_6a);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 6, context, R.raw.diesel_7fdl_idle_7a);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 7, context, R.raw.diesel_7fdl_idle_8a);
                  mainapp.soundsLocoSteps[throttleIndex] = 7; // fast steam
                  break;

               case "dieselNW2":
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 0, context, R.raw.diesel_nw7_motor);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 1, context, R.raw.diesel_nw7_motor_2);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 2, context, R.raw.diesel_nw7_motor_1);
                  mainapp.soundsLocoSteps[throttleIndex] = 2;
                  break;

               case "steamClass64":
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 0, context, R.raw.steam_class64_idle_sound);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 1, context, R.raw.steam_class64_chuff1_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 2, context, R.raw.steam_class64_chuff2_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 3, context, R.raw.steam_class64_chuff3_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 4, context, R.raw.steam_class64_chuff4_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 5, context, R.raw.steam_class64_chuff5_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 6, context, R.raw.steam_class64_chuff6_1_4);
                  mainapp.soundsLocoSteps[throttleIndex] = 6;
                  break;

               case "steamClass94":
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 0, context, R.raw.steam_class94_idle2a);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 1, context, R.raw.steam_class94_speed0a_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 2, context, R.raw.steam_class94_speed2g_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 3, context, R.raw.steam_class94_speed3g_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 4, context, R.raw.steam_class94_speed4g_1_4);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, 5, context, R.raw.steam_class94_speed5g_1_4);
                  mainapp.soundsLocoSteps[throttleIndex] = 5;
                  break;
            }

            switch (mainapp.prefDeviceSounds[throttleIndex]) {
               default:
                  mainapp.soundsLoco[throttleIndex][SOUNDS_STARTUP_INDEX] = 0;
                  mainapp.soundsLocoDuration[throttleIndex][SOUNDS_STARTUP_INDEX] = 0;
                  mainapp.soundsLoco[throttleIndex][SOUNDS_SHUTDOWN_INDEX] = 0;
                  mainapp.soundsLocoDuration[throttleIndex][SOUNDS_SHUTDOWN_INDEX] = 0;
                  break;
               case "diesel645turbo":
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, SOUNDS_STARTUP_INDEX, context, R.raw.diesel_645turbo_start);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, SOUNDS_SHUTDOWN_INDEX, context, R.raw.diesel_645turbo_shutdown);
                  break;

               case "dieselNW2":
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, SOUNDS_STARTUP_INDEX, context, R.raw.diesel_nw7_start_22050);
                  loadSound(SOUNDS_TYPE_LOCO, throttleIndex, SOUNDS_SHUTDOWN_INDEX, context, R.raw.diesel_nw7_stop_22050);
                  break;

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
         Toast.makeText(context, R.string.toastInitialisingSounds, Toast.LENGTH_LONG).show();
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
         case SOUNDS_TYPE_LOCO: // loco
            mainapp.soundsLoco[whichThrottle][soundNo] = mainapp.soundPool.load(context, resId, 1);
            mainapp.soundsLocoDuration[whichThrottle][soundNo] = duration;
            break;
         case SOUNDS_TYPE_BELL: // bell
         case SOUNDS_TYPE_HORN: // horn
         case SOUNDS_TYPE_HORN_SHORT: // horn short
            mainapp.soundsExtras[soundType-1][whichThrottle][soundNo] = mainapp.soundPool.load(context, resId, 1);
            mainapp.soundsExtrasDuration[soundType-1][whichThrottle][soundNo] = duration;
            break;
      }
   } // end loadSound()

   void loadSoundFromFile(int soundType, int whichThrottle, int soundNo, Context context, String fileName) {
        Log.d("Engine_Driver", "loadSoundFromFile (locoSound): file:" + fileName + " wt: " + whichThrottle + " sNo: " + soundNo);
      int duration = 0;

      if (fileName.length() > 0) {
         File file = new File(context.getExternalFilesDir(null), fileName);

         MediaPlayer player = MediaPlayer.create(context, Uri.fromFile(file));
         if (player != null)
            duration = player.getDuration();
         soundsCountOfSoundBeingLoaded ++;

         switch (soundType) {
            default:
            case SOUNDS_TYPE_LOCO: // loco
               mainapp.soundsLoco[whichThrottle][soundNo]
                       = mainapp.soundPool.load(context.getExternalFilesDir(null) + "/" + fileName, 1);
               mainapp.soundsLocoDuration[whichThrottle][soundNo] = duration;
               break;
            case SOUNDS_TYPE_BELL: // bell
            case SOUNDS_TYPE_HORN: // horn
            case SOUNDS_TYPE_HORN_SHORT: // horn short
               mainapp.soundsExtras[soundType-1][whichThrottle][soundNo]
                       = mainapp.soundPool.load(context.getExternalFilesDir(null) + "/" + fileName, 1);
               mainapp.soundsExtrasDuration[soundType-1][whichThrottle][soundNo] = duration;
               break;
         }
//         Log.d("Engine_Driver", "loadSoundFromFile (locoSound) : file loaded: " + fileName + " wt: " + whichThrottle + " sNo: " + soundNo);
      } else {
         switch (soundType) {
            default:
            case SOUNDS_TYPE_LOCO: // loco
               mainapp.soundsLoco[whichThrottle][soundNo] = 0;
               mainapp.soundsLocoDuration[whichThrottle][soundNo] = 0;
               break;
            case SOUNDS_TYPE_BELL: // bell
            case SOUNDS_TYPE_HORN: // horn
            case SOUNDS_TYPE_HORN_SHORT: // horn
               mainapp.soundsExtras[soundType-1][whichThrottle][soundNo] = 0;
               mainapp.soundsExtrasDuration[soundType-1][whichThrottle][soundNo] = 0;
               break;
         }
      }
   } // end loadSoundsFromFile()

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
        int x=1;
    }

   public void getIplsDetails(String fileName) {
      String name = "";
      String cmd;
      int num;
      iplsLocoSoundsCount =-1;

      File iplsFile = new File(context.getExternalFilesDir(null), fileName);
      if (iplsFile.exists()) {
         BufferedReader list_reader = null;
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
                                    num = SOUNDS_STARTUP_INDEX;
                                 } else if (line.substring(1, splitPos).equals("-")) { // shutdown sound
                                    num = SOUNDS_SHUTDOWN_INDEX;
                                 } else {
                                    break;
                                 }
                              }
                           }
                           if ((num >= 0) && (num <= SOUNDS_SHUTDOWN_INDEX)) {
                              iplsLocoSoundsFileName[num] = line.substring(splitPos + 1, line.length() - splitPos + 2).trim();
                              if ( (num > iplsLocoSoundsCount) && (num < SOUNDS_STARTUP_INDEX) ) {  // don't count the Startup and shutdown sounds
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
                                    num = SOUNDS_STARTUP_INDEX;
                                 } else if (line.substring(1, splitPos).equals("-")) { // shutdown sound
                                    num = SOUNDS_SHUTDOWN_INDEX;
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
      Log.d("Engine_Driver", "getRecentLocosListFromFile: ImportExportPreferences: Read recent locos list from file complete successfully");
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

      for (int i = 0; i < iplsLocoSoundsFileName.length; i++) {
         iplsLocoSoundsFileName[i] = "";
      }
      for (int i = 0; i < 3; i++) {
         iplsBellSoundsFileName[i] = "";
         iplsHornSoundsFileName[i] = "";
      }
      iplsHornShortSoundsFileName = "";
      iplsLocoSoundsCount = -1;
      iplsName = "";   // name for the menues
      iplsFileName = "";

   } // end clearAllSounds

}
