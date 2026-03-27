package jmri.enginedriver.util;

import android.content.SharedPreferences;
import android.util.Log;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.sounds_type;

public class InPhoneLocoSounds {
    private final String activityName = "InPhoneLocoSounds";
    private final SharedPreferences prefs;
    private final threaded_application mainapp;

    public InPhoneLocoSounds(threaded_application mainapp, SharedPreferences prefs) {
        this.prefs = prefs;
        this.mainapp = mainapp;
        loadPrefs();
    }

    private void loadPrefs() {
//        Log.d(threaded_application.applicationName, activityName + ": loadPrefs : (ipls)" );
        mainapp.prefDeviceSounds[0] = prefs.getString("prefDeviceSounds0",
                mainapp.getResources().getString(R.string.prefDeviceSoundsDefaultValue));
        mainapp.prefDeviceSounds[1] = prefs.getString("prefDeviceSounds1",
                mainapp.getResources().getString(R.string.prefDeviceSoundsDefaultValue));
        mainapp.prefDeviceSoundsMomentum = threaded_application.getIntPrefValue(prefs, "prefDeviceSoundsMomentum", mainapp.getResources().getString(R.string.prefDeviceSoundsMomentumDefaultValue));
        mainapp.prefDeviceSoundsMomentumOverride = prefs.getBoolean("prefDeviceSoundsMomentumOverride",
                mainapp.getResources().getBoolean(R.bool.prefDeviceSoundsMomentumOverrideDefaultValue));
        mainapp.prefDeviceSoundsLocoVolume = threaded_application.getIntPrefValue(prefs, "prefDeviceSoundsLocoVolume", "100");
        mainapp.prefDeviceSoundsBellVolume = threaded_application.getIntPrefValue(prefs, "prefDeviceSoundsBellVolume", "100");
        mainapp.prefDeviceSoundsHornVolume = threaded_application.getIntPrefValue(prefs, "prefDeviceSoundsHornVolume", "100");
        mainapp.prefDeviceSoundsLocoVolume = mainapp.prefDeviceSoundsLocoVolume / 100;
        mainapp.prefDeviceSoundsBellVolume = mainapp.prefDeviceSoundsBellVolume / 100;
        mainapp.prefDeviceSoundsHornVolume = mainapp.prefDeviceSoundsHornVolume / 100;

        mainapp.prefDeviceSoundsBellIsMomentary = prefs.getBoolean("prefDeviceSoundsBellIsMomentary",
                mainapp.getResources().getBoolean(R.bool.prefDeviceSoundsBellIsMomentaryDefaultValue));
        mainapp.prefDeviceSoundsF1F2ActivateBellHorn = prefs.getBoolean("prefDeviceSoundsF1F2ActivateBellHorn",
                mainapp.getResources().getBoolean(R.bool.prefDeviceSoundsF1F2ActivateBellHornDefaultValue));
        mainapp.prefDeviceSoundsHideMuteButton = prefs.getBoolean("prefDeviceSoundsHideMuteButton",
                mainapp.getResources().getBoolean(R.bool.prefDeviceSoundsHideMuteButtonDefaultValue));
    }

    public class DoLocoSoundDelayed implements Runnable {
        int whichThrottle;
        int currentSpeed;
        int dir;
        boolean soundsAreMutedForThisThrottle;

        public DoLocoSoundDelayed(int whichThrottle, int currentSpeed, int dir, boolean soundsAreMutedForThisThrottle) {
            this.whichThrottle = whichThrottle;
            this.currentSpeed = currentSpeed;
            this.dir = dir;
            this.soundsAreMutedForThisThrottle = soundsAreMutedForThisThrottle;
        }

        @Override
        public void run() {
//            Log.d(threaded_application.applicationName, activityName + ": doLocoSoundDelayed(): run: (ipls) wt" + whichThrottle);
            doLocoSound(whichThrottle, currentSpeed, dir, soundsAreMutedForThisThrottle);
        }
    } // end DoLocoSoundDelayed()

    public void doLocoSound(int whichThrottle, int currentSpeed, int dir, boolean soundsAreMutedForThisThrottle) {
//        Log.d(threaded_application.applicationName, activityName + ": doLocoSound : (ipls) wt: " + whichThrottle + " " + mainapp.soundsLocoQueue[whichThrottle].displayQueue());
        if (!mainapp.soundsSoundsAreBeingReloaded) {
            if (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES) { // only dealing with the first two throttle for now
                if (!mainapp.prefDeviceSounds[whichThrottle].equals("none")) {
                    int mSound = -1;
                    if ((mainapp.consists != null) && (mainapp.consists[whichThrottle].isActive())) {
                        mSound = getLocoSoundStep(whichThrottle, currentSpeed);

//                        Log.d(threaded_application.applicationName, activityName + ": doLocoSound               : (ipls) wt: " + whichThrottle + " snd: " + mSound);
                        if ((mSound >= 0)) {
                            if (mainapp.soundsLocoCurrentlyPlaying[whichThrottle] == sounds_type.NOTHING_CURRENTLY_PLAYING) { // nothing currently playing
//                                Log.d(threaded_application.applicationName, activityName + ": doLocoSound 2              : (ipls) wt: " + whichThrottle + " snd: " + mSound);
                                //see if there is a startup sound for this profile
                                if (mainapp.soundsLocoDuration[whichThrottle][sounds_type.STARTUP_INDEX] > 0) {
//                                    Log.d(threaded_application.applicationName, activityName + ": doLocoSound 3              : (ipls) wt: " + whichThrottle + " snd: " + mSound);
                                    soundStart(sounds_type.LOCO, whichThrottle, sounds_type.STARTUP_INDEX, sounds_type.REPEAT_NONE, soundsAreMutedForThisThrottle);
                                    scheduleNextLocoSound(whichThrottle, mSound, mainapp.soundsLocoDuration[whichThrottle][sounds_type.STARTUP_INDEX], soundsAreMutedForThisThrottle);
                                    queueNextLocoSound(whichThrottle, mSound, dir, soundsAreMutedForThisThrottle);
                                } else {
//                                    Log.d(threaded_application.applicationName, activityName + ": doLocoSound 4              : (ipls) wt: " + whichThrottle + " snd: " + mSound);
                                    soundStart(sounds_type.LOCO, whichThrottle, mSound, sounds_type.REPEAT_INFINITE, soundsAreMutedForThisThrottle);
                                    mainapp.soundsLocoQueue[whichThrottle].setLastAddedValue(mSound);
                                }

                            } else {
                                queueNextLocoSound(whichThrottle, mSound, dir, soundsAreMutedForThisThrottle);
                            }
                        }
                    } else {
                        stopAllSoundsForLoco(whichThrottle);
                    }
                }
                mainapp.soundsLocoLastDirection[whichThrottle] = dir;
            }
        }
    } // end doLocoSound()

    private void queueNextLocoSound(int whichThrottle, int mSound, int dir, boolean soundsAreMutedForThisThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": queueNextLocoSound : (ipls) wt: " + whichThrottle + " snd: " + mSound + " " + mainapp.soundsLocoQueue[whichThrottle].displayQueue());
        boolean wasDirectionChange = mainapp.soundsLocoLastDirection[whichThrottle] != dir;

        if ((mainapp.soundsLocoCurrentlyPlaying[whichThrottle] == mSound) && (mainapp.soundsLocoQueue[whichThrottle].queueCount() == 0) && (!wasDirectionChange)) {
            return; // sound is already playing and nothing is queued
        }
//        Log.d(threaded_application.applicationName, activityName + ": soundQueueNextLocoSound  : (ipls) wt: " + whichThrottle + " snd: " + mSound + " " + mainapp.soundsLocoQueue[whichThrottle].displayQueue());

        int queueCount = mainapp.soundsLocoQueue[whichThrottle].queueCount();

        if (mainapp.soundsLocoQueue[whichThrottle].enqueueWithIntermediateSteps(mSound, wasDirectionChange)) {
            if (queueCount == 0) {
                scheduleNextLocoSound(whichThrottle, mSound, -1, soundsAreMutedForThisThrottle);
            }
        }
    } // end queueNextLocoSound()

    public void scheduleNextLocoSound(int whichThrottle, int mSound, int forcedExpectedEndTime, boolean soundsAreMutedForThisThrottle) {
//        Log.d(threaded_application.applicationName, activityName + ": scheduleNextLocoSound : (ipls) wt: " + whichThrottle + " snd: " + mSound + " " + mainapp.soundsLocoQueue[whichThrottle].displayQueue());

        int expectedEndTime;
        if (forcedExpectedEndTime > 0) {
            expectedEndTime = forcedExpectedEndTime;
        } else { // != -1
            expectedEndTime = soundStop(sounds_type.LOCO, whichThrottle, mainapp.soundsLocoCurrentlyPlaying[whichThrottle], false);
        }
        int nextSound = mainapp.soundsLocoQueue[whichThrottle].frontOfQueue();

        if (nextSound >= 0) {
            if (mainapp.activityBundleMessageHandlers[activity_id_type.THROTTLE] != null)
                mainapp.activityBundleMessageHandlers[activity_id_type.THROTTLE].postDelayed(
                    new ScheduleNextSoundToPlay(sounds_type.LOCO, whichThrottle, nextSound),
                    expectedEndTime - 100);
//            Log.d(threaded_application.applicationName, activityName + ": soundScheduleNextLocoSound : (ipls) wt:" + whichThrottle + " snd: " + nextSound + " Start in: " + expectedEndTime + "msec");
        }
    } //end scheduleNextLocoSound()

    public void stopAllSoundsForLoco(int whichThrottle) {
//        Log.d(threaded_application.applicationName, activityName + ": stopAllSoundsForLoco(): (ipls) wt: " + whichThrottle);
        if (mainapp.soundPool != null) {
            for (int i = 0; i < 17; i++) {
                mainapp.soundPool.stop(mainapp.soundsLocoStreamId[whichThrottle][i]);
            }
            mainapp.soundsLocoQueue[whichThrottle].emptyQueue();
            for (int soundType = 0; soundType < 3; soundType++) {
                for (int i = 0; i < 3; i++) {
                    mainapp.soundPool.stop(mainapp.soundsExtrasStreamId[soundType][whichThrottle][i]);
                }
            }
            mainapp.soundsLocoCurrentlyPlaying[whichThrottle] = -1;
        }
    } // end stopAllSoundsForLoco

    public int getLocoSoundStep(int whichThrottle, int currentSpeed) {
        int rslt;
        int steps = mainapp.soundsLocoSteps[whichThrottle];

        float speed = (float) (currentSpeed);
        speed = (float) ((speed / 126 * steps) + 0.99);
        rslt = (int) speed;

//        Log.d(threaded_application.applicationName, activityName + ": getLocoSoundStep         : (ipls) wt: " + whichThrottle + " step:" + rslt);
        return rslt;
    } // end getLocoSoundStep()

    // this is not used for Loco sounds
    public int soundIsPlaying(int soundType, int whichThrottle) {
        int isPlaying;
        int soundTypeArrayIndex = soundType - 1;

        switch (soundType) {
            case sounds_type.BELL: // bell
            case sounds_type.HORN: // horn
            case sounds_type.HORN_SHORT: // horn short
                isPlaying = mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle];
                break;
            default:
                isPlaying = -1;
                break;
        }
        return isPlaying;
    } // end soundIsPlaying()

    public void startBellHornSound(int soundType, int whichThrottle, boolean soundsAreMutedForThisThrottle) {
//        Log.d(threaded_application.applicationName, activityName + ": startBellHornSound        : soundType:" + soundType + " wt: " + whichThrottle);
        int soundTypeArrayIndex = soundType - 1;

        if (soundIsPlaying(soundType, whichThrottle) < 0) { // check if the loop sound is not currently playing
            if (soundType != sounds_type.HORN_SHORT) {
                soundStart(soundType, whichThrottle, sounds_type.BELL_HORN_START, 0, soundsAreMutedForThisThrottle);
                // queue up the loop sound to be played when the start sound is finished
                if (mainapp.activityBundleMessageHandlers[activity_id_type.THROTTLE] != null)
                    mainapp.activityBundleMessageHandlers[activity_id_type.THROTTLE].postDelayed(
                        new ScheduleNextSoundToPlay(soundType, whichThrottle, sounds_type.BELL_HORN_START),
                        mainapp.soundsExtrasDuration[soundTypeArrayIndex][whichThrottle][sounds_type.BELL_HORN_START]);
            } else {
                soundStart(soundType, whichThrottle, 0, 0, soundsAreMutedForThisThrottle);   // mSound always 0 for the short horn (not really used)
            }
        }
    } // end startBellHornSound()

    public void stopBellHornSound(int soundType, int whichThrottle, boolean soundsAreMutedForThisThrottle) {
//        Log.d(threaded_application.applicationName, activityName + ": stopBellHornSound        : soundType:" + soundType + " wt: " + whichThrottle + " playing: " + soundIsPlaying(soundType,whichThrottle));
        int soundTypeArrayIndex = soundType - 1;

        if (soundType != sounds_type.HORN_SHORT) {
            if (soundIsPlaying(soundType, whichThrottle) == 0) {
                // if the start sound is currently playing need to do something special
                // as the loop is probably scheduled to run but has not started yet
                if (mainapp.activityBundleMessageHandlers[activity_id_type.THROTTLE] != null)
                    mainapp.activityBundleMessageHandlers[activity_id_type.THROTTLE].postDelayed(
                        new ScheduleSoundToStop(soundType, whichThrottle, sounds_type.BELL_HORN_LOOP),
                        mainapp.soundsExtrasDuration[soundTypeArrayIndex][whichThrottle][sounds_type.BELL_HORN_START] + 100);

            } else if (soundIsPlaying(soundType, whichThrottle) == 1) { // check if the loop sound is currently playing
                int expectedEndTime = soundStop(soundType, whichThrottle, sounds_type.BELL_HORN_LOOP, false);
                // queue up the end sound
                if (mainapp.activityBundleMessageHandlers[activity_id_type.THROTTLE] != null)
                    mainapp.activityBundleMessageHandlers[activity_id_type.THROTTLE].postDelayed(
                        new ScheduleNextSoundToPlay(soundType, whichThrottle, sounds_type.BELL_HORN_LOOP),
                        expectedEndTime);
            }
        } else { // for the short horn, stop immediately
            soundStop(soundType, whichThrottle, 0, true);
        }
    } // end stopBellHornSound(

    public void soundStart(int soundType, int whichThrottle, int mSound, int loop, boolean soundsAreMutedForThisThrottle) {
//        Log.d(threaded_application.applicationName, activityName + ": soundStart: SoundType:" + soundType + " wt: " + whichThrottle + " snd: " + mSound + " loop:" + loop);
        int soundTypeArrayIndex = soundType - 1;

        switch (soundType) {
            case sounds_type.BELL: // bell
            case sounds_type.HORN: // horn
            case sounds_type.HORN_SHORT: // horn short
                if (mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] != mSound) { // nothing playing
                    mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound]
                            = mainapp.soundPool.play(mainapp.soundsExtras[soundTypeArrayIndex][whichThrottle][mSound],
                            volume(soundType, whichThrottle, soundsAreMutedForThisThrottle),
                            volume(soundType, whichThrottle, soundsAreMutedForThisThrottle),
                            0, loop, 1);
                    mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] = (mSound < 2) ? mSound : -1; // if it is the end sound, treat it like is not playing
                    mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][mSound] = System.currentTimeMillis();
                }
                break;

            case sounds_type.LOCO: // loco
            default:
//                Log.d(threaded_application.applicationName, activityName + ": soundStart               : (ipls) wt: " + whichThrottle + " snd: " + mSound + " loop:" + loop);
                if (mSound >= 0) {
                    if (mainapp.soundsLocoCurrentlyPlaying[whichThrottle] != mSound) {
                        if (mSound < sounds_type.STARTUP_INDEX) {
                            mainapp.soundsLocoStreamId[whichThrottle][mSound]
                                    = mainapp.soundPool.play(mainapp.soundsLoco[whichThrottle][mSound],
                                    volume(sounds_type.LOCO, whichThrottle, soundsAreMutedForThisThrottle),
                                    volume(sounds_type.LOCO, whichThrottle, soundsAreMutedForThisThrottle),
                                    0, sounds_type.REPEAT_INFINITE, 1);
                        } else if (mSound == sounds_type.STARTUP_INDEX) {
                            mainapp.soundsLocoStreamId[whichThrottle][mSound]
                                    = mainapp.soundPool.play(mainapp.soundsLoco[whichThrottle][mSound],
                                    volume(sounds_type.LOCO, whichThrottle, soundsAreMutedForThisThrottle),
                                    volume(sounds_type.LOCO, whichThrottle, soundsAreMutedForThisThrottle),
                                    0, sounds_type.REPEAT_NONE, 1);
//                            Log.d(threaded_application.applicationName, activityName + ": soundStart SU            : (ipls) wt: " + whichThrottle + " snd: " + mSound + " loop:" + loop + " Sid: " + mainapp.soundsLocoStreamId[whichThrottle][mSound]);

//                        } else if (mSound == sounds_type.SHUTDOWN_INDEX) {
                        }
                        mainapp.soundsLocoCurrentlyPlaying[whichThrottle] = mSound;
                        mainapp.soundsLocoStartTime[whichThrottle][mSound] = System.currentTimeMillis();
                    }
                }
                break;
        }
    }  // end startSound()

    public int soundStop(int soundType, int whichThrottle, int mSound, boolean forceStop) {
//        Log.d(threaded_application.applicationName, activityName + ": soundStop: soundType" + soundType + " wt: " + whichThrottle + " snd: " + mSound);
        int timesPlayed = 0;
        double expectedEndTime = 0;
        int soundTypeArrayIndex = soundType - 1;

        switch (soundType) {
            case sounds_type.BELL: // bell
            case sounds_type.HORN: // horn
                if (mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] >= 0) {
                    if (mSound == sounds_type.BELL_HORN_LOOP) { // assume it is looping
                        mainapp.soundPool.pause(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound]);
                        mainapp.soundPool.setLoop(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound], sounds_type.REPEAT_NONE);  // don't really stop it, just let it finish
                        mainapp.soundPool.resume(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound]);
                        timesPlayed = (int) ((System.currentTimeMillis() - mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][mSound])
                                / mainapp.soundsExtrasDuration[soundTypeArrayIndex][whichThrottle][mSound]);
                        expectedEndTime = (timesPlayed) * mainapp.soundsExtrasDuration[soundTypeArrayIndex][whichThrottle][mSound];
                        expectedEndTime = mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][mSound] + expectedEndTime - System.currentTimeMillis();
                    } else {
                        mainapp.soundPool.stop(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound]);
                    }
                    mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] = -1;
                    mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][mSound] = 0;
                }
                break;

            case sounds_type.HORN_SHORT: // horn short
                if (mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] >= 0) {
                    mainapp.soundPool.stop(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][0]);
                    mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] = -1;
                    mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][0] = 0;
                }
                break;

            case sounds_type.LOCO: // loco
            default:
//                Log.d(threaded_application.applicationName, activityName + ": soundStop                : (ipls) wt: " + whichThrottle + " mSound: " + snd + " forceStop:" + forceStop);
                if (mSound >= 0) {
                    if (mSound < sounds_type.STARTUP_INDEX) {
                        if (!forceStop) {
                            double duration = mainapp.soundsLocoDuration[whichThrottle][mSound];

                            // number of complete completed loops
                            timesPlayed = (int) ((System.currentTimeMillis() - mainapp.soundsLocoStartTime[whichThrottle][mSound])
                                    / mainapp.soundsLocoDuration[whichThrottle][mSound]);
                            expectedEndTime = (timesPlayed + 1) * mainapp.soundsLocoDuration[whichThrottle][mSound];

                            int repeats = 1;
                            if (expectedEndTime < mainapp.prefDeviceSoundsMomentum) {
                                repeats = 2;
                            }
                            if ((duration * (repeats + 1)) < mainapp.prefDeviceSoundsMomentum) { // if the sound is less than the preference period 1 second will need to repeat it
                                repeats = (int) (mainapp.prefDeviceSoundsMomentum / duration) + 1;
                            }
//                              Log.d(threaded_application.applicationName, activityName + ": soundStop                : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " expected to end in: " +(x/1000)+"sec" );

                            mainapp.soundPool.pause(mainapp.soundsLocoStreamId[whichThrottle][mSound]); // unfortunately you seem to have to pause it to change the number of repeats
                            mainapp.soundPool.setLoop(mainapp.soundsLocoStreamId[whichThrottle][mSound], repeats);  // don't really stop it, just let it finish
                            mainapp.soundPool.resume(mainapp.soundsLocoStreamId[whichThrottle][mSound]);

                            if (mainapp.prefDeviceSoundsMomentumOverride) {
                                expectedEndTime = (timesPlayed + repeats) * mainapp.soundsLocoDuration[whichThrottle][mSound];
                                expectedEndTime = mainapp.soundsLocoStartTime[whichThrottle][mSound] + expectedEndTime - System.currentTimeMillis();
                            } else {
                                expectedEndTime = mainapp.prefDeviceSoundsMomentum;  // schedule the next sound for the preference amount regardless
                            }

//                        Log.d(threaded_application.applicationName, activityName + ": soundStop                : (ipls) wt: " + whichThrottle + " snd: " + mSound + " timesPlayed:" + timesPlayed + " will end in: " +(expectedEndTime/1000)+"sec" );
                        } else {
                            mainapp.soundPool.stop(mainapp.soundsLocoStreamId[whichThrottle][mSound]);
//                        Log.d(threaded_application.applicationName, activityName + ": soundStop                : (ipls) wt: " + whichThrottle + " snd: " + mSound + " timesPlayed:" + timesPlayed + " FORCED STOP");
                        }
                    } else if (mSound == sounds_type.STARTUP_INDEX) {
                        // this will only ever play once
                        if (!forceStop) {
                            expectedEndTime = mainapp.soundsLocoStartTime[whichThrottle][mSound] + mainapp.soundsLocoDuration[whichThrottle][mSound] - System.currentTimeMillis();
//                            Log.d(threaded_application.applicationName, activityName + ": soundStop                : (ipls) wt: " + whichThrottle + " snd: " + mSound + " timesPlayed:" + timesPlayed + " will end in: " +(expectedEndTime/1000)+"sec" );
                        } else {
                            mainapp.soundPool.stop(mainapp.soundsLocoStreamId[whichThrottle][mSound]);
//                            Log.d(threaded_application.applicationName, activityName + ": soundStop                : (ipls) wt: " + whichThrottle + " snd: " + mSound + " timesPlayed:" + timesPlayed + " FORCED STOP");
                        }

                    }
                }
                break;

        }
        return (int) expectedEndTime;
    } // end stopSound()

    // used for the loco sounds only
    public class ScheduleNextSoundToPlay implements Runnable {
        int whichThrottle;
        int soundType;
        int mSound;
        boolean soundsAreMutedForThisThrottle;

        public ScheduleNextSoundToPlay(int soundType, int whichThrottle, int mSound) {
            this.whichThrottle = whichThrottle;
            this.soundType = soundType;
            this.mSound = mSound;
            soundsAreMutedForThisThrottle = mainapp.soundsIsMuted[whichThrottle];
        }

        @Override
        public void run() {
//            Log.d(threaded_application.applicationName, activityName + ": SoundScheduleNextSoundToPlay.run: Type" + soundType + " wt: " + whichThrottle + " snd: " + mSound);

            soundsAreMutedForThisThrottle = mainapp.soundsIsMuted[whichThrottle]; // recheck each loop

            switch (soundType) {
                case sounds_type.LOCO: // loco
                    // pull the next sound off the queue
//                    Log.d(threaded_application.applicationName, activityName + ": ScheduleNextSoundToPlay.run: (ipls) wt: " + whichThrottle + " snd: " + mSound);
                    soundStop(soundType, whichThrottle, mainapp.soundsLocoCurrentlyPlaying[whichThrottle], true);
                    soundStart(soundType, whichThrottle, mSound, sounds_type.REPEAT_INFINITE, soundsAreMutedForThisThrottle);
                    mainapp.soundsLocoQueue[whichThrottle].dequeue();
                    if (mainapp.soundsLocoQueue[whichThrottle].queueCount() > 0) {  // if there are more on the queue, start the process to stop the one you just started
                        scheduleNextLocoSound(whichThrottle, mainapp.soundsLocoQueue[whichThrottle].frontOfQueue(), -1, soundsAreMutedForThisThrottle);
                    }
                    break;
                case sounds_type.BELL: // bell
                case sounds_type.HORN: // horn
                    int loop = (mSound == sounds_type.BELL_HORN_START) ? sounds_type.REPEAT_INFINITE : sounds_type.REPEAT_NONE; // of 0 now, then we will be playig the lop sound next.
                    soundStart(soundType, whichThrottle, mSound + 1, loop, soundsAreMutedForThisThrottle);
                    break;
//                case sounds_type.HORN_SHORT:
//                    break;
                default:
                    break;
            }
        }
    } // end class SoundScheduleNextSoundToPlay

    public class ScheduleSoundToStop implements Runnable {
        int whichThrottle;
        int soundType;
        int mSound;

        public ScheduleSoundToStop(int soundType, int whichThrottle, int mSound) {
            this.whichThrottle = whichThrottle;
            this.soundType = soundType;
            this.mSound = mSound;
        }

        @Override
        public void run() {
//            Log.d(threaded_application.applicationName, activityName + ": ScheduleSoundToStop.run: Type" + soundType + " wt: " + whichThrottle + " snd: " + mSound);
            switch (soundType) {
                case sounds_type.HORN: // horn
                case sounds_type.BELL: // bell
                    soundStop(soundType, whichThrottle, mSound, false);
                    break;
//                case sounds_type.HORN_SHORT:
//                    break;
                default:
                    break;
            }
        }
    } // end class SoundScheduleSoundToStop

    public float volume(int soundType, int whichThrottle, boolean soundsAreMutedForThisThrottle) {
        float volume = 0;
        if ((whichThrottle < mainapp.maxThrottlesCurrentScreen) && (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES)) {
            if (!soundsAreMutedForThisThrottle) {
                switch (soundType) {
                    case sounds_type.BELL: // bell
                        volume = mainapp.prefDeviceSoundsBellVolume;
                        break;
                    case sounds_type.HORN: // horn
                    case sounds_type.HORN_SHORT: // horn
                        volume = mainapp.prefDeviceSoundsHornVolume;
                        break;
                    case sounds_type.LOCO: // loco
                    default:
                        volume = mainapp.prefDeviceSoundsLocoVolume;
                        break;
                }
            }
        }
        return volume;
    }

    public void muteUnmuteCurrentSounds(int whichThrottle, boolean soundsAreMutedForThisThrottle) {
        if (mainapp.soundPool != null) {
            if (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES) {
                int mSound = mainapp.soundsLocoCurrentlyPlaying[whichThrottle];
                if (mSound >= 0) {
                    mainapp.soundPool.setVolume(mainapp.soundsLocoStreamId[whichThrottle][mSound],
                           volume(sounds_type.LOCO, whichThrottle, soundsAreMutedForThisThrottle),
                           volume(sounds_type.LOCO, whichThrottle, soundsAreMutedForThisThrottle));
                }
                for (int soundType = 0; soundType < 2; soundType++) {  // don't worry about the short horn
                    if (mainapp.soundsExtrasCurrentlyPlaying[soundType][whichThrottle] == sounds_type.BELL_HORN_LOOP) {
                        mainapp.soundPool.setVolume(mainapp.soundsExtrasStreamId[soundType][whichThrottle][sounds_type.BELL_HORN_LOOP],
                                volume(soundType + 1, whichThrottle, soundsAreMutedForThisThrottle),
                                volume(soundType + 1, whichThrottle, soundsAreMutedForThisThrottle));
                    }
                }
            }
        }
    }

}
