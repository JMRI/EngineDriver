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

public interface activity_id_type {
    int NONE = 0;
    int THROTTLE = 1;
    int ROUTES = 2;
    int TURNOUTS = 3;
    int ABOUT = 4;
    int CONNECTION = 5;
    int CONSIST_EDIT = 6;
    int CONSIST_LIGHTS_EDIT = 7;
    int DCC_EX = 8;
    int DEVICE_SOUNDS_SETTINGS = 9;
    int FUNCTION_CONSIST_SETTINGS = 10;
    int FUNCTION_SETTINGS = 11;
    int GAMEPAD_TEST = 12;
    int POWER_CONTROL = 13;
    int RECONNECT_STATUS = 14;
    int SELECT_LOCO = 15;
    int SETTINGS = 16;
    int WEB = 17;
    int WITHROTTLE_CV_PROGRAMMER = 18;
    int INTRO = 19;
    int LOG_VIEWER = 20;
}
