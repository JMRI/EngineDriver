package jmri.enginedriver.util;

import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
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

    private SharedPreferences prefs;
    private ImageView image;
    private threaded_application mainapp;

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
//                            Toast.makeText(mainapp, mainapp.getResources().getString(R.string.toastTtsFailed), Toast.LENGTH_SHORT).show();
                            threaded_application.safeToast(mainapp.getResources().getString(R.string.toastTtsFailed), Toast.LENGTH_SHORT);
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
        if (!prefTtsWhen.equals(PREF_TTS_WHEN_NONE)) {
            if (myTts != null) {
                switch (msgNo) {
                    case tts_msg_type.VOLUME_THROTTLE:
                        if (!prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_NONE)) {
                            int displaySpeedFromCurrentSliderPosition = argInt1; //getDisplaySpeedFromCurrentSliderPosition(whichThrottle,true)
                            String consistAddressString = argString; // getConsistAddressString(whichThrottle);

                            if (whichLastVolume != whichThrottle) {
                                result = true;
                                whichLastVolume = whichThrottle;
                                speech = mainapp.getResources().getString(R.string.TtsVolumeThrottle) + " " + (whichThrottle + 1);
                            }
                            if ((prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO)) || (prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsLoco) + " " + (consistAddressString);
                            }
                            if ((prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_SPEED)) || (prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsSpeed) + " "
                                        + (displaySpeedFromCurrentSliderPosition);
                            }
                        }
                        break;
                    case tts_msg_type.GAMEPAD_THROTTLE:
                        if (!prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_NONE)) {
                            int whichLastGamepad1 = argInt1;
                            int speed = argInt2; // getDisplaySpeedFromCurrentSliderPosition(whichThrottle,true)
                            int targetSpeed = argInt4; // getDisplaySpeedFromCurrentSliderPosition(whichThrottle,true)
                            String consistAddressString = argString; // getConsistAddressString(whichThrottle)

                            if ((whichLastGamepad1 != whichThrottle) || (force)) {
                                result = true;
                                whichLastGamepad1 = whichThrottle;
                                speech = mainapp.getResources().getString(R.string.TtsGamepadThrottle) + " " + (whichThrottle + 1);
                            }
                            if ((prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO)) || (prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsLoco) + " " + (consistAddressString);
                            }
                            if ((prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_SPEED)) || (prefTtsThrottleResponse.equals(pref_tts_type.THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsSpeed) + " "
                                        + (speed);
                                if (isSemiRealisticThrottle) {
                                    speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsTargetSpeed) + " "
                                            + (targetSpeed);
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
                    case tts_msg_type.GAMEPAD_THROTTLE_SPEED:
                        if ( (prefTtsThrottleSpeed.equals("Zero + Max")) || (prefTtsThrottleSpeed.equals("Zero + Max + speed")) ) {
                            int maxSpd = argInt1; // getMaxSpeed(whichThrottle);
                            int spd = argInt2;    // getSpeedFromCurrentSliderPosition(whichThrottle,false)
                            int speed = argInt3;    // getSpeedFromCurrentSliderPosition(whichThrottle,true)
                            int targetSpeed = argInt4;    // getSpeedFromCurrentSliderPosition(whichThrottle,true)

                            if (spd == 0) {
                                result = true;
                                speech = mainapp.getResources().getString(R.string.TtsGamepadTestSpeedZero);
                            } else if (spd == maxSpd) {
                                result = true;
                                speech = mainapp.getResources().getString(R.string.TtsGamepadTestSpeedMax);
                                if ((prefTtsThrottleSpeed.equals("Zero + Max + speed"))) {
                                    speech = speech + " " + speed;
                                }
                            }
                            if (isSemiRealisticThrottle) {
                                speech = speech  + ", " + mainapp.getResources().getString(R.string.TtsTargetSpeed) + " "
                                        + (targetSpeed);
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
