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
 */

package jmri.enginedriver.util;

import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.pref_tts_type;
import jmri.enginedriver.type.tts_msg_type;

public class Tts {
    private static final String PREF_TTS_WHEN_NONE = "None";

    private String lastTts = "none";
    private String prefTtsWhen = "None";
    private long lastTtsTime;
    private int whichLastVolume = -1;

    private TextToSpeech myTts;

    private String prefTtsThrottleResponse;
    private String prefTtsThrottleSpeed;
    private boolean prefTtsGamepadTest;
    private boolean prefTtsGamepadTestComplete;
    private boolean prefTtsGamepadTestKeys;

    private final SharedPreferences prefs;
    private final threaded_application mainapp;

    public Tts(SharedPreferences myPrefs, threaded_application myMainapp) {
        prefs = myPrefs;
        mainapp = myMainapp;
        loadPrefs();

        if (!prefTtsWhen.equals(mainapp.getResources().getString(R.string.prefTtsWhenDefaultValue))) {
            if (myTts == null) {
//                lastTtsTime = new Time();
//                lastTtsTime.setToNow();
                lastTtsTime = System.currentTimeMillis();

                myTts = new TextToSpeech(mainapp, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            myTts.setLanguage(Locale.getDefault());
                        } else {
                            mainapp.safeToast(mainapp.getResources().getString(R.string.toastTtsFailed), Toast.LENGTH_SHORT);
                        }
                    }
                });
            }
        }
    }

    public void loadPrefs() {
        prefTtsWhen = prefs.getString("prefTtsWhen", mainapp.getString(R.string.prefTtsWhenDefaultValue));
        prefTtsThrottleResponse = prefs.getString("prefTtsThrottleResponse", mainapp.getString(R.string.prefTtsThrottleResponseDefaultValue));
        prefTtsThrottleSpeed = prefs.getString("prefTtsThrottleSpeed", mainapp.getString(R.string.prefTtsThrottleSpeedDefaultValue));
        prefTtsGamepadTest = prefs.getBoolean("prefTtsGamepadTest", mainapp.getResources().getBoolean(R.bool.prefTtsGamepadTestDefaultValue));
        prefTtsGamepadTestComplete = prefs.getBoolean("prefTtsGamepadTestComplete", mainapp.getResources().getBoolean(R.bool.prefTtsGamepadTestCompleteDefaultValue));
        prefTtsGamepadTestKeys = prefs.getBoolean("prefTtsGamepadTestKeys", mainapp.getResources().getBoolean(R.bool.prefTtsGamepadTestKeysDefaultValue));
    }

    public void speakWords(int msgNo) {
        speakWords(msgNo, ' ', false, 0, 0, 0, 0, false, "");
    }

    public void speakWords(int msgNo, String argString) {
        speakWords(msgNo, ' ', false, 0, 0, 0, 0, false, argString);
    }

    public void speakWords(int msgNo, int whichThrottle) {
        speakWords(msgNo, whichThrottle, false, 0, 0, 0, 0, false, "");
    }

    public void speakWords(int msgNo, int whichThrottle, boolean force) {
        speakWords(msgNo, whichThrottle, force, 0, 0, 0, 0, false, "");
    }

    public void speakWords(int msgNo, int whichThrottle, boolean force, int argInt1, int argInt2, int argInt3, String argString) {
        speakWords(msgNo, whichThrottle, force, argInt1, argInt2, argInt3, 0, false, argString);
    }

    public void speakWords(int msgNo, int whichThrottle, boolean force, int argInt1, int argInt2, int argInt3,  int argInt4, boolean isSemiRealisticThrottle, String argString) {
        boolean result = false;
        String speech = "";
        if ( (!prefTtsWhen.equals(PREF_TTS_WHEN_NONE)) || (force) ) {
            if (myTts != null) {
                switch (msgNo) {
                    case tts_msg_type.VOLUME_THROTTLE:
                        if (!prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_NONE)) {
                            if (whichLastVolume != whichThrottle) {
                                result = true;
                                whichLastVolume = whichThrottle;
                                speech = mainapp.getResources().getString(R.string.TtsVolumeThrottle) + " " + (whichThrottle + 1);
                            }
                            if ((prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO)) || (prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsLoco) + " " + (argString); // consistAddressString
                            }
                            if ((prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_SPEED)) || (prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsSpeed) + " "
                                        + (argInt1);  // displaySpeedFromCurrentSliderPosition
                            }
                        }
                        break;

                    case tts_msg_type.GAMEPAD_THROTTLE:
                        if (!prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_NONE)) {
                            if ((argInt1 != whichThrottle) || (force)) { // whichLastGamepad1
                                result = true;
//                                whichLastGamepad1 = whichThrottle;
                                speech = mainapp.getResources().getString(R.string.TtsGamepadThrottle) + " " + (whichThrottle + 1);
                            }
                            if ((prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO)) || (prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsLoco) + " " + (argString); // consistAddressString
                            }
                            if ((prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_SPEED)) || (prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsSpeed) + " "
                                        + (argInt2); // speed
                                if (isSemiRealisticThrottle) {
                                    speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsTargetSpeed) + " "
                                            + (argInt4); // targetSpeed
                                }
                            }
                        }
                        break;

                    case tts_msg_type.GAMEPAD_GAMEPAD_TEST:
                        if ((prefTtsGamepadTest)) {
                            result = true;
                            speech = mainapp.getResources().getString(R.string.TtsGamepadTest);
                        }
                        break;

                    case tts_msg_type.GAMEPAD_GAMEPAD_TEST_COMPLETE:
                        if ((prefTtsGamepadTestComplete)) {
                            result = true;
                            speech = mainapp.getResources().getString(R.string.TtsGamepadTestComplete);
                        }
                        break;

                    case tts_msg_type.GAMEPAD_GAMEPAD_TEST_SKIPPED:
                        if ((prefTtsGamepadTestComplete)) {
                            result = true;
                            speech = mainapp.getResources().getString(R.string.TtsGamepadTestSkipped);
                        }
                        break;

                    case tts_msg_type.GAMEPAD_GAMEPAD_TEST_FAIL:
                        if ((prefTtsGamepadTestComplete)) {
                            result = true;
                            speech = mainapp.getResources().getString(R.string.TtsGamepadTestFail);
                        }
                        break;

                    case tts_msg_type.GAMEPAD_GAMEPAD_TEST_RESET:
                        if ((prefTtsGamepadTestComplete)) {
                            result = true;
                            speech = mainapp.getResources().getString(R.string.TtsGamepadTestReset);
                        }
                        break;

                    case tts_msg_type.GAMEPAD_GAMEPAD_TEST_KEY_AND_PURPOSE:
                        if (prefTtsGamepadTestKeys) {
                            result = true;
                            speech = argString;
                        }
                        break;

                    case tts_msg_type.GAMEPAD_CURRENT_SPEED:
                        result = true;
                        speech = speech + " " + mainapp.getResources().getString(R.string.TtsSpeed) + " " + argInt2 ; // speed
                        if (argInt2>0)
                            speech = speech + ", " + ((argInt3==1) ? mainapp.getResources().getString(R.string.TTS_Forward) : mainapp.getResources().getString(R.string.TTS_Reverse)); // direction
                        if (isSemiRealisticThrottle) {
                            speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsTargetSpeed) + " "
                                    + (argInt4); // targetSpeed
                        }
                        break;

                    case tts_msg_type.GAMEPAD_THROTTLE_SPEED:
                        if ( (prefTtsThrottleSpeed.equals("Zero + Max")) || (prefTtsThrottleSpeed.equals("Zero + Max + speed")) || (force) )  {
                            if (argInt2 == 0) { // speed zero
                                result = true;
                                speech = mainapp.getResources().getString(R.string.TtsGamepadTestSpeedZero);
                            } else if (argInt2 == argInt1) { // spd == maxSpd
                                result = true;
                                speech = mainapp.getResources().getString(R.string.TtsGamepadTestSpeedMax);
                                if ((prefTtsThrottleSpeed.equals("Zero + Max + speed"))) {
                                    speech = speech + " " + argInt3; // speed
                                }
                            }
                            if (isSemiRealisticThrottle) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsTargetSpeed) + " "
                                        + (argInt4); // targetSpeed
                            }
                        }
                        break;
                }

                if (result) {
//                    Time currentTime = new Time();
//                    currentTime.setToNow();
//                    if (((currentTime.toMillis(true) >= (lastTtsTime.toMillis(true) + 1500)) || (!speech.equals(lastTts)))) {

                    // //don't repeat what was last spoken within 1.5 seconds
                    if (((System.currentTimeMillis() >= (lastTtsTime + 1500)) || (!speech.equals(lastTts)))) {
                        //myTts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
                        myTts.speak(speech, TextToSpeech.QUEUE_ADD, null);
//                        lastTtsTime = currentTime;
                        lastTtsTime = System.currentTimeMillis();
                    }
                    lastTts = speech;
                }
            }
        }
    }

}
