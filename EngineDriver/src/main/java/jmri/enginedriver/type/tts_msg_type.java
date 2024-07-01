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
*/

package jmri.enginedriver.type;

public interface tts_msg_type {
    int VOLUME_THROTTLE = 1;
    int GAMEPAD_THROTTLE = 2;
    int GAMEPAD_GAMEPAD_TEST = 3;
    int GAMEPAD_GAMEPAD_TEST_COMPLETE = 4;
    int GAMEPAD_GAMEPAD_TEST_SKIPPED = 5;
    int GAMEPAD_GAMEPAD_TEST_RESET = 6;
    int GAMEPAD_GAMEPAD_TEST_FAIL = 7;
    int GAMEPAD_GAMEPAD_TEST_KEY_AND_PURPOSE = 8;
    int GAMEPAD_THROTTLE_SPEED = 9;
}