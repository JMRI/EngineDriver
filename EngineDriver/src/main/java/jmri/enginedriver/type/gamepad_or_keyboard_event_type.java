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

public interface gamepad_or_keyboard_event_type {
    int NONE                         =  0;
    int ESTOP                        =  1;
    int STOP                         =  2;
    int STOP_ALL                     =  3;
    int FORWARD                      =  4;
    int REVERSE                      =  5;
    int TOGGLE_DIRECTION             =  6;
    int INCREASE_SPEED_START         =  7;
//    int INCREASE_SPEED_END           =  8;
    int DECREASE_SPEED_START         =  9;
//    int DECREASE_SPEED_END           = 10;
    int NEXT_THROTTLE                = 11;
//    int CHANGE_THROTTLE              = 12;
    int SRT_NEUTRAL                  = 13;
    int SRT_BRAKE_INCREASE           = 14;
    int SRT_BRAKE_DECREASE           = 15;
    int SRT_LOAD_INCREASE            = 16;
    int SRT_LOAD_DECREASE            = 17;
    int IPLS_MUTE                    = 18;
    int IPLS_BELL_TOGGLE             = 29;
    int IPLS_HORN_START              = 20;
    int IPLS_HORN_END                = 21;
    int IPLS_HORN_SHORT_START        = 22;
    int IPLS_HORN_SHORT_END          = 24;
    int LIMIT_SPEED_TOGGLE           = 25;
    int PAUSE_SPEED_TOGGLE           = 26;
    int SPEAK_SPEED                  = 27;
    int FUNCTION_START               = 28;
    int FUNCTION_END                 = 39;
    int FUNCTION_FORCED_LATCH_START  = 30;
    int FUNCTION_FORCED_LATCH_END    = 31;
    int SPEED                        = 32;
}
