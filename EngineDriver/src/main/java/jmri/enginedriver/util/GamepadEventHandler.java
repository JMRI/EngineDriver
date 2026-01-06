/*Copyright (C) 2018 M. Steve Todd mstevetodd@gmail.com

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

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;

import android.util.Log;

import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.gamepad_or_keyboard_event_type;
import jmri.enginedriver.type.pref_gamepad_button_option_type;

public class GamepadEventHandler {
    static final String activityName = "GamepadEventHandler";
    threaded_application mainapp;
    private final KeyboardNotifierInterface keyboardNotifierInterface;

    private static final String GAMEPAD_FUNCTION_PREFIX = "Function ";

    String keyboardString = "";
    int tempKeyboardThrottle = -1;
    int keyboardStopCount = 0;
    int MAX_SCREEN_THROTTLES = 1;
    private int whichLastGamepadButtonPressed = -1;
    private String prefGamePadDoublePressStop = "";
    private long gamePadDoublePressStopTime;

    private int[] gamePadKeys;
    private int[] gamePadKeys_Up;
    private String[] prefGamePadButtons;

    public GamepadEventHandler(threaded_application mainapp,
                               KeyboardNotifierInterface keyboardNotifierInterface,
                               int maxScreenThrottles,
                               int[] gamePadKeys,
                               int[] gamePadKeys_Up,
                               String[] prefGamePadButtons,
                               String prefGamePadDoublePressStop) {
        this.keyboardNotifierInterface = keyboardNotifierInterface;
        this.mainapp = mainapp;
        MAX_SCREEN_THROTTLES = maxScreenThrottles;
        this.gamePadKeys = gamePadKeys;
        this.gamePadKeys_Up = gamePadKeys_Up;
        this.prefGamePadButtons = prefGamePadButtons;
    }

    public void handleGamepadEvent(int buttonNo, int action,
                                    int repeatCnt, int originalWhichThrottle,
                                    boolean isActive,
                                    int gamepadThrottle,
                                    boolean isGamepadThrottleActive,
                                    boolean isSemiRealisticThrottle,
                                    int whichGamePadIsEventFrom) {

        Log.d(threaded_application.applicationName, activityName + ": handleGamepadEvent() action: " + action);

        if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.ALL_STOP)) {  // All Stop
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.STOP_ALL, 0, repeatCnt,
                        0, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.STOP)) {  // Stop
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.STOP, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);

                if ((whichLastGamepadButtonPressed == buttonNo)
                        && (System.currentTimeMillis() <= (gamePadDoublePressStopTime + 1000))) {  // double press - within 1 second
                    if (prefGamePadDoublePressStop.equals(pref_gamepad_button_option_type.ALL_STOP)) {
                        keyboardNotifierInterface.keyboardEventNotificationHandler(
                                gamepad_or_keyboard_event_type.STOP_ALL, 0, repeatCnt,
                                gamepadThrottle, isActive, whichGamePadIsEventFrom);
                    } else if (prefGamePadDoublePressStop.equals(pref_gamepad_button_option_type.FORWARD_REVERSE_TOGGLE)) {
//                        boolean dirChangeFailed = !changeActualOrTargetDirectionIfAllowed(whichThrottle,
//                                getDirection(whichThrottle) == direction_type.FORWARD ? direction_type.REVERSE : direction_type.FORWARD,
//                                gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle));
                        keyboardNotifierInterface.keyboardEventNotificationHandler(
                                gamepad_or_keyboard_event_type.TOGGLE_DIRECTION, 0, repeatCnt,
                                gamepadThrottle, isActive, whichGamePadIsEventFrom);

//                        if (!isSemiRealisticThrottle) {
//                            if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
//                                dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
//                            } else {
//                                dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
//                            }
//                        } else { // semi-realistic throttle variant
//// -----------
//// need to figure out neutral!!!
//                            if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
//                                dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
//                            } else {
//                                dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
//                            }
//                        }
//                        GamepadFeedbackSound(dirChangeFailed);
                    } // else do nothing
                    whichLastGamepadButtonPressed = -1;  // reset the count
                    gamePadDoublePressStopTime = 0; // reset the time
                } else {
                    gamePadDoublePressStopTime = System.currentTimeMillis();
                }
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.NEXT_THROTTLE)) {  // Next Throttle
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.NEXT_THROTTLE, 0, repeatCnt,
                        0, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.FORWARD)) {  // Forward
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.FORWARD, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.REVERSE)) {  // Reverse
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.REVERSE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.FORWARD_REVERSE_TOGGLE)) {  // Toggle Forward/Reverse
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.TOGGLE_DIRECTION, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.INCREASE_SPEED)) {  // Increase Speed
            if (isActive && (action == ACTION_DOWN)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.INCREASE_SPEED_START, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.DECREASE_SPEED)) {  // Decrease Speed
            if (isActive && (action == ACTION_DOWN)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.DECREASE_SPEED_START, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SOUNDS_MUTE)) {  // IPLS Sounds - Mute
            if (isActive && (action == ACTION_UP)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.IPLS_MUTE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SOUNDS_BELL)) {  // IPLS Sounds - Bell
            if (isActive && (action == ACTION_UP)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.IPLS_BELL_TOGGLE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SOUNDS_HORN)) {  // IPLS Sounds - Horn
            if (isActive) {
                if (action == ACTION_UP) {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.IPLS_HORN_END, 0, repeatCnt,
                            gamepadThrottle, isActive, whichGamePadIsEventFrom);
                } else {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.IPLS_HORN_START, 0, repeatCnt,
                            gamepadThrottle, isActive, whichGamePadIsEventFrom);
                }
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SOUNDS_HORN_SHORT)) {  // IPLS Sounds - Horn Short
            if (isActive) {
                if (action == ACTION_UP) {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.IPLS_HORN_SHORT_END, 0, repeatCnt,
                            gamepadThrottle, isActive, whichGamePadIsEventFrom);
                } else {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.IPLS_HORN_START, 0, repeatCnt,
                            gamepadThrottle, isActive, whichGamePadIsEventFrom);
                }
            }

        } else if ((prefGamePadButtons[buttonNo].length() >= 11) && (prefGamePadButtons[buttonNo].startsWith(GAMEPAD_FUNCTION_PREFIX))) { // one of the Function Buttons
            int fKey = Integer.parseInt(prefGamePadButtons[buttonNo].substring(9, 11));
            if (isActive && (repeatCnt == 0)) {
                if (action == ACTION_DOWN) {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.FUNCTION_START, fKey, repeatCnt,
                            gamepadThrottle, isActive, whichGamePadIsEventFrom);
                } else {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.FUNCTION_END, fKey, repeatCnt,
                            gamepadThrottle, isActive, whichGamePadIsEventFrom);
                }
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.LIMIT_SPEED)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.LIMIT_SPEED_TOGGLE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.PAUSE)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.PAUSE_SPEED_TOGGLE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SPEAK_CURRENT_SPEED)) {
//            tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE, whichThrottle, true
//                    , whichLastGamepad1
//                    , getDisplaySpeedFromCurrentSliderPosition(whichThrottle, true)
//                    , 0
//                    , getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
//                    , isSemiRealisticThrottle
//                    , getConsistAddressString(whichThrottle));
            keyboardNotifierInterface.keyboardEventNotificationHandler(
                    gamepad_or_keyboard_event_type.SPEAK_SPEED, 0, repeatCnt,
                    gamepadThrottle, isActive, whichGamePadIsEventFrom);

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.NEUTRAL)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_NEUTRAL, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.INCREASE_BRAKE)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_BRAKE_INCREASE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.DECREASE_BRAKE)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_BRAKE_DECREASE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.INCREASE_LOAD)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_LOAD_INCREASE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.DECREASE_LOAD)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_LOAD_DECREASE, 0, repeatCnt,
                        gamepadThrottle, isActive, whichGamePadIsEventFrom);
            }
//        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.NONE)) {
//             do nothing
        }

        whichLastGamepadButtonPressed = buttonNo;
    }

}

