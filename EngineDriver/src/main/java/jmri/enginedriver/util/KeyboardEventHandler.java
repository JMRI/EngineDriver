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
import static android.view.KeyEvent.KEYCODE_0;
import static android.view.KeyEvent.KEYCODE_9;
import static android.view.KeyEvent.KEYCODE_APOSTROPHE;
import static android.view.KeyEvent.KEYCODE_B;
import static android.view.KeyEvent.KEYCODE_BACKSLASH;
import static android.view.KeyEvent.KEYCODE_COMMA;
import static android.view.KeyEvent.KEYCODE_D;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_EQUALS;
import static android.view.KeyEvent.KEYCODE_F;
import static android.view.KeyEvent.KEYCODE_F1;
import static android.view.KeyEvent.KEYCODE_F10;
import static android.view.KeyEvent.KEYCODE_F11;
import static android.view.KeyEvent.KEYCODE_G;
import static android.view.KeyEvent.KEYCODE_H;
import static android.view.KeyEvent.KEYCODE_I;
import static android.view.KeyEvent.KEYCODE_L;
import static android.view.KeyEvent.KEYCODE_LEFT_BRACKET;
import static android.view.KeyEvent.KEYCODE_M;
import static android.view.KeyEvent.KEYCODE_MEDIA_NEXT;
import static android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;
import static android.view.KeyEvent.KEYCODE_MINUS;
import static android.view.KeyEvent.KEYCODE_MOVE_END;
import static android.view.KeyEvent.KEYCODE_MOVE_HOME;
import static android.view.KeyEvent.KEYCODE_N;
import static android.view.KeyEvent.KEYCODE_NUMPAD_ADD;
import static android.view.KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
import static android.view.KeyEvent.KEYCODE_O;
import static android.view.KeyEvent.KEYCODE_P;
import static android.view.KeyEvent.KEYCODE_PAGE_DOWN;
import static android.view.KeyEvent.KEYCODE_PAGE_UP;
import static android.view.KeyEvent.KEYCODE_PERIOD;
import static android.view.KeyEvent.KEYCODE_PLUS;
import static android.view.KeyEvent.KEYCODE_RIGHT_BRACKET;
import static android.view.KeyEvent.KEYCODE_S;
import static android.view.KeyEvent.KEYCODE_SEMICOLON;
import static android.view.KeyEvent.KEYCODE_T;
import static android.view.KeyEvent.KEYCODE_U;
import static android.view.KeyEvent.KEYCODE_V;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_MUTE;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.KeyEvent.KEYCODE_X;
import static android.view.KeyEvent.KEYCODE_Z;
import static jmri.enginedriver.threaded_application.MAX_FUNCTIONS;

import android.util.Log;

import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.gamepad_or_keyboard_event_type;
import jmri.enginedriver.type.sounds_type;

public class KeyboardEventHandler {
    static final String activityName = "KeyboardEventHandler";
    threaded_application mainapp;
    private final KeyboardNotifierInterface keyboardNotifierInterface;

    String keyboardString = "";
    int tempGamepadOrKeyboardThrottle = -1;
    int tempKeyboardTurnout = -1;
    int keyboardStopCount = 0;
    int MAX_SCREEN_THROTTLES = 1;

    public KeyboardEventHandler(threaded_application mainapp, KeyboardNotifierInterface keyboardNotifierInterface, int maxScreenThrottles) {
        this.keyboardNotifierInterface = keyboardNotifierInterface;
        this.mainapp = mainapp;
        MAX_SCREEN_THROTTLES = maxScreenThrottles;
    }

    public void handleKeyboardEvent(int keyCode, int action,
                                    boolean isShiftPressed, int repeatCnt, int originalWhichThrottle,
                                    boolean isActive,
                                    int gamepadOrKeyboardThrottle,
                                    boolean isKeyboardThrottleActive,
                                    boolean isSemiRealisticThrottle,
                                    int whichGamePadIsEventFrom) {

        Log.d(threaded_application.applicationName, activityName + ": handleKeyboardEvent() action: " + action);
        int whichThrottle = originalWhichThrottle;
        if ((gamepadOrKeyboardThrottle >= 0) && (gamepadOrKeyboardThrottle != originalWhichThrottle)) {
            whichThrottle = gamepadOrKeyboardThrottle;
            isActive = isKeyboardThrottleActive;
        }
        if ((tempGamepadOrKeyboardThrottle >= 0) && (tempGamepadOrKeyboardThrottle != originalWhichThrottle)) {
            whichThrottle = tempGamepadOrKeyboardThrottle;
        }

        if ((keyCode == KEYCODE_Z) || (keyCode == KEYCODE_MOVE_END)) {  // E Stop

            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.ESTOP, 0, repeatCnt,
                        whichThrottle,   isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_X) || (keyCode == KEYCODE_MOVE_HOME)) {  // Stop
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardStopCount++;
                if (!isSemiRealisticThrottle) {
                    if (keyboardStopCount==1) {
                        keyboardNotifierInterface.keyboardEventNotificationHandler(
                                gamepad_or_keyboard_event_type.STOP, 0, repeatCnt,
                                whichThrottle, isActive, whichGamePadIsEventFrom);
                    } else { // Estop all
                        keyboardNotifierInterface.keyboardEventNotificationHandler(
                                gamepad_or_keyboard_event_type.ESTOP, 0, repeatCnt,
                                whichThrottle, isActive, whichGamePadIsEventFrom);
                        keyboardStopCount = 0;
                    }
                } else {
                    // assumes only one Throttle
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.STOP, 0, repeatCnt,
                            whichThrottle, isActive, whichGamePadIsEventFrom);
                }
                resetKeyboardString();
            }
        } else if (keyCode == KEYCODE_N) {  // Next Throttle
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.NEXT_THROTTLE, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_DPAD_RIGHT) || (keyCode == KEYCODE_RIGHT_BRACKET)) {  // Forward
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.FORWARD, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_DPAD_LEFT) || (keyCode == KEYCODE_LEFT_BRACKET)) {  // Reverse
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.REVERSE, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if (keyCode == KEYCODE_D) {  // Toggle Forward/Reverse
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.TOGGLE_DIRECTION, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_DPAD_UP) || (keyCode == KEYCODE_PLUS)
                || (keyCode == KEYCODE_EQUALS) || (keyCode == KEYCODE_NUMPAD_ADD)
                || (keyCode == KEYCODE_VOLUME_UP)) {  // Increase Speed
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.INCREASE_SPEED_START, 1, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_DPAD_DOWN) || (keyCode == KEYCODE_MINUS)
                || (keyCode == KEYCODE_NUMPAD_SUBTRACT)
                || (keyCode == KEYCODE_VOLUME_DOWN)) {  // Decrease Speed
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.DECREASE_SPEED_START, 1, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((isSemiRealisticThrottle) && (keyCode == KEYCODE_BACKSLASH)) { // semi-realistic throttle - neutral
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_NEUTRAL, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
            }
        } else if ((isSemiRealisticThrottle) && ((keyCode == KEYCODE_PAGE_UP) || (keyCode == KEYCODE_APOSTROPHE)) ) { // semi-realistic throttle - increase brake
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_BRAKE_INCREASE, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
            }
        } else if ((isSemiRealisticThrottle) && ((keyCode == KEYCODE_PAGE_DOWN) || (keyCode == KEYCODE_SEMICOLON)) ) { // semi-realistic throttle - decrease brake
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_BRAKE_DECREASE, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
            }
        } else if ((isSemiRealisticThrottle) && (keyCode == KEYCODE_COMMA) ) { // semi-realistic throttle - increase load
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_LOAD_INCREASE, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
            }
        } else if ((isSemiRealisticThrottle) && (keyCode == KEYCODE_PERIOD)) { // semi-realistic throttle - decrease load
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SRT_LOAD_DECREASE, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
            }
        } else if ((keyCode == KEYCODE_MEDIA_NEXT)) {  // Increase Speed * 2
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.INCREASE_SPEED_START, 2, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_MEDIA_PREVIOUS)) {  // Decrease Speed * 2
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.DECREASE_SPEED_START, 2, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_VOLUME_MUTE) || (keyCode == KEYCODE_M)) {  // IPLS Sounds - Mute
            if (isActive && (action == ACTION_UP) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.IPLS_MUTE, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if (keyCode == KEYCODE_B) {  // IPLS Sounds - Bell
            if (isActive && (action == ACTION_UP) && (repeatCnt == 0)) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.IPLS_BELL_TOGGLE, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_H) && (!isShiftPressed)) {  // IPLS Sounds - Horn
            if (isActive) {
                if (action == ACTION_UP) {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.IPLS_HORN_END, 0, repeatCnt,
                            whichThrottle, isActive, whichGamePadIsEventFrom);
                    resetKeyboardString();
                } else {
                    if (repeatCnt == 0) {
                        if (!mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1]) {
                            keyboardNotifierInterface.keyboardEventNotificationHandler(
                                    gamepad_or_keyboard_event_type.IPLS_HORN_START, 0, repeatCnt,
                                    whichThrottle, isActive, whichGamePadIsEventFrom);
                        }
                    }
                }
            }
        } else if ((keyCode == KEYCODE_H) && (isShiftPressed)) {  // IPLS Sounds - Horn Short
            if (isActive) {
                if (action == ACTION_UP) {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.IPLS_HORN_SHORT_END, 0, repeatCnt,
                            whichThrottle, isActive, whichGamePadIsEventFrom);
                    resetKeyboardString();
                } else {
                    if (repeatCnt == 0) {
                        if (!mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1]) {
                            keyboardNotifierInterface.keyboardEventNotificationHandler(
                                    gamepad_or_keyboard_event_type.IPLS_HORN_SHORT_START, 0, repeatCnt,
                                    whichThrottle, isActive, whichGamePadIsEventFrom);
                        }
                    }
                }
            }
        } else if (keyCode == KEYCODE_L) { // Limit Speed toggle
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (!isSemiRealisticThrottle) {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.LIMIT_SPEED_TOGGLE, 0, repeatCnt,
                            whichThrottle, isActive, whichGamePadIsEventFrom);
                } // not available on semi-realistic throttle
                resetKeyboardString();
            }
        } else if (keyCode == KEYCODE_P) { // pause speed toggle
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (!isSemiRealisticThrottle) {
                    keyboardNotifierInterface.keyboardEventNotificationHandler(
                            gamepad_or_keyboard_event_type.PAUSE_SPEED_TOGGLE, 0, repeatCnt,
                            whichThrottle, isActive, whichGamePadIsEventFrom);
                }
                resetKeyboardString();
            }

        } else if (keyCode == KEYCODE_V) {  // Speak speed
            if (action == ACTION_DOWN) {
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        gamepad_or_keyboard_event_type.SPEAK_SPEED, 0, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
            }

        } else if ( (keyCode == KEYCODE_F) || (keyCode == KEYCODE_F11) ) {  // Start of a Function command
            if (action == ACTION_DOWN) {
                keyboardString = "F";
            }
        } else if (keyCode == KEYCODE_G) {  // Start of a Forced Function command
            if (action == ACTION_DOWN) {
                keyboardString = "G";
            }
        } else if (keyCode == KEYCODE_S) {  // Start of a speed command
            if (action == ACTION_DOWN) {
                keyboardString = "S";
            }
        } else if (keyCode == KEYCODE_T) {  // Start of a throttle specification
            if (action == ACTION_DOWN) {
                keyboardString = "T";
                tempGamepadOrKeyboardThrottle = -1;  //reset it
            }
        } else if (keyCode == KEYCODE_U) {  // Start of a turnout/point specification
            if (action == ACTION_DOWN) {
                keyboardString = "U";
                tempKeyboardTurnout = -1;  //reset it
            }
        } else if (keyCode == KEYCODE_I) {  // Start of a turnout/point specification
            if (action == ACTION_DOWN) {
                keyboardString = "I";
                tempKeyboardTurnout = -1;  //reset it
            }
        } else if (keyCode == KEYCODE_O) {  // Start of a turnout/point specification
            if (action == ACTION_DOWN) {
                keyboardString = "O";
                tempKeyboardTurnout = -1;  //reset it
            }
        } else if (((keyCode >= KEYCODE_0) && (keyCode <= KEYCODE_9))
                || ((keyCode >= KEYCODE_F1) && (keyCode <= KEYCODE_F10))) {  // Start of a Function, Speed, or Throttle command
            String num;
            if ((keyCode >= KEYCODE_0) && (keyCode <= KEYCODE_9)) {
                num = Integer.toString(keyCode - KEYCODE_0);
            } else {
                if (keyCode != KEYCODE_F10) {
                    num = Integer.toString(keyCode - KEYCODE_F1 + 1);
                } else {
                    num = "0";
                }
            }

            if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'F')) {  // Function
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                }
                if (keyboardString.length() == 3) {  // have a two digit function number now
                    int fKey = Integer.parseInt(keyboardString.substring(1, 3));
                    if (fKey < MAX_FUNCTIONS) {
                        if (action == ACTION_DOWN) {
                            keyboardNotifierInterface.keyboardEventNotificationHandler(
                                    gamepad_or_keyboard_event_type.FUNCTION_START, fKey, repeatCnt,
                                    whichThrottle, isActive, whichGamePadIsEventFrom);
                        } else {
                            keyboardNotifierInterface.keyboardEventNotificationHandler(
                                    gamepad_or_keyboard_event_type.FUNCTION_END, fKey, repeatCnt,
                                    whichThrottle, isActive, whichGamePadIsEventFrom);
                            resetKeyboardString();
                        }
                    }
                }
            } else if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'G')) {  // Forced Latching Function
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                }
                if (keyboardString.length() == 3) {  // have a two digit function number now
                    int fKey = Integer.parseInt(keyboardString.substring(1, 3));
                    if (fKey < MAX_FUNCTIONS) {
                        if (action == ACTION_DOWN) {
                            keyboardNotifierInterface.keyboardEventNotificationHandler(
                                    gamepad_or_keyboard_event_type.FUNCTION_FORCED_LATCH_START, fKey, repeatCnt,
                                    whichThrottle, isActive, whichGamePadIsEventFrom);
                        } else {
                            keyboardNotifierInterface.keyboardEventNotificationHandler(
                                    gamepad_or_keyboard_event_type.FUNCTION_FORCED_LATCH_END, fKey, repeatCnt,
                                    whichThrottle, isActive, whichGamePadIsEventFrom);
                            resetKeyboardString();
                        }
                    }
                }
            } else if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'S')) {  // speed
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                    if (keyboardString.length() == 4) {  // have a three digit speed amount now
                        int newSpeed = Math.min(Integer.parseInt(keyboardString.substring(1, 4)), 100);
                        float vSpeed = 126 * ((float) newSpeed) / 100;
                        newSpeed = (int) vSpeed;

                        keyboardNotifierInterface.keyboardEventNotificationHandler(
                                gamepad_or_keyboard_event_type.SPEED, newSpeed, repeatCnt,
                                whichThrottle, isActive, whichGamePadIsEventFrom);
                    }
                }
                if (action == ACTION_UP) {
                    if (keyboardString.length() == 4) {  // have a three digit speed amount now
                        resetKeyboardString();
                    }
                }

            } else if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'U')) {  // Toggle Turnout/Point
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                    if (keyboardString.length() == 4) {  // have a three digit turnout address now
                        tempKeyboardTurnout = Integer.parseInt(keyboardString.substring(1, 4));

                        keyboardNotifierInterface.keyboardEventNotificationHandler(
                                gamepad_or_keyboard_event_type.TURNOUT_TOGGLE, tempKeyboardTurnout, repeatCnt,
                                whichThrottle, isActive, whichGamePadIsEventFrom);
                    }
                }
                if (action == ACTION_UP) {
                    if (keyboardString.length() == 4) {  // have a three digit turnout address now
                        resetKeyboardString();
                    }
                }
            } else if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'I')) {  // Throw Turnout/Point
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                    if (keyboardString.length() == 4) {  // have a three digit turnout address now
                        tempKeyboardTurnout = Integer.parseInt(keyboardString.substring(1, 4));

                        keyboardNotifierInterface.keyboardEventNotificationHandler(
                                gamepad_or_keyboard_event_type.TURNOUT_THROW, tempKeyboardTurnout, repeatCnt,
                                whichThrottle, isActive, whichGamePadIsEventFrom);
                    }
                }
                if (action == ACTION_UP) {
                    if (keyboardString.length() == 4) {  // have a three digit turnout address now
                        resetKeyboardString();
                    }
                }
            } else if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'O')) {  // CLOSE Turnout/Point
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                    if (keyboardString.length() == 4) {  // have a three digit turnout address now
                        tempKeyboardTurnout = Integer.parseInt(keyboardString.substring(1, 4));

                        keyboardNotifierInterface.keyboardEventNotificationHandler(
                                gamepad_or_keyboard_event_type.TURNOUT_CLOSE, tempKeyboardTurnout, repeatCnt,
                                whichThrottle, isActive, whichGamePadIsEventFrom);
                    }
                }
                if (action == ACTION_UP) {
                    if (keyboardString.length() == 4) {  // have a three digit turnout address now
                        resetKeyboardString();
                    }
                }

            } else if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'T')) {    // specify Throttle number
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                }
                if (action == ACTION_DOWN) {
                    if (keyboardString.length() == 2) {  // have a complete Throttle number
                        tempGamepadOrKeyboardThrottle = Integer.parseInt(keyboardString.substring(1, 2));
                        if (tempGamepadOrKeyboardThrottle > MAX_SCREEN_THROTTLES) {
                            tempGamepadOrKeyboardThrottle = -1;
                        }
                    }
                } else {
                    keyboardString = "";
                }
            } else if (keyboardString.isEmpty()) {  // direct function 0-9
                int fKey;
                if ((keyCode >= KEYCODE_0) && (keyCode <= KEYCODE_9)) {
                    fKey = keyCode - KEYCODE_0;
                } else {
                    if (keyCode != KEYCODE_F10) {
                        fKey = keyCode - KEYCODE_F1 + 1;
                    } else {
                        fKey = 0;
                    }
                }
                if (fKey < 10) { // special case for attached keyboards keys 0-9
                    mainapp.numericKeyIsPressed[fKey] = action;
                    if (action == ACTION_DOWN) {
                        mainapp.numericKeyFunctionStateAtTimePressed[fKey] = ((mainapp.function_states[whichThrottle][fKey]) ? 1 : 0);
                    }
                }
                keyboardNotifierInterface.keyboardEventNotificationHandler(
                        (action == ACTION_DOWN) ? gamepad_or_keyboard_event_type.FUNCTION_START : gamepad_or_keyboard_event_type.FUNCTION_END,
                        fKey, repeatCnt,
                        whichThrottle, isActive, whichGamePadIsEventFrom);
                if (action == ACTION_UP) {
                    resetKeyboardString();
                }
            }
        }

        if ((keyCode != KEYCODE_X) && (keyCode != KEYCODE_MOVE_HOME)) {  // if the key was anything other than a stop, reset the count
            keyboardStopCount = 0;
        }
    }

    void resetKeyboardString() {
        Log.d(threaded_application.applicationName, activityName + ": resetKeyboardString()");
        keyboardString = "";
        tempGamepadOrKeyboardThrottle = -1;  //reset it
    }

}

