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
    int THROTTLE =                   0;
    int ROUTES =                     1;
    int TURNOUTS =                   2;
    int ABOUT =                      3;
    int CONNECTION =                 4;
    int CONSIST_EDIT =               5;
    int CONSIST_LIGHTS_EDIT =        6;
    int DCC_EX =                     7;
    int DEVICE_SOUNDS_SETTINGS =     8;
    int FUNCTION_CONSIST_SETTINGS =  9;
    int FUNCTION_SETTINGS =         10;
    int GAMEPAD_TEST =              11;
    int POWER_CONTROL =             12;
    int RECONNECT_STATUS =          13;
    int SELECT_LOCO =               14;
    int SETTINGS =                  15;
    int WEB =                       16;
    int WITHROTTLE_CV_PROGRAMMER =  17;
    int INTRO =                     18;
    int LOG_VIEWER =                19;
    int ADVANCED_CONSIST_TOOL =     20;

    // these allow for additional handlers that are not included broadcasts to all activities
    int MIN =                        0;
    int MAX =                       20;
}
