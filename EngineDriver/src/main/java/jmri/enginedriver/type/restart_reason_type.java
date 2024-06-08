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

public interface restart_reason_type {
    int NONE = 0;
    int RESET = 1;
    int IMPORT = 2;
    int IMPORT_SERVER_MANUAL = 3;
    int THEME = 4;
    int THROTTLE_PAGE = 5;
    int LOCALE = 6;
    int IMPORT_SERVER_AUTO = 7;
    int AUTO_IMPORT = 8; // for local server files
    int BACKGROUND = 9;
    int THROTTLE_SWITCH = 10;
    int FORCE_WIFI = 11;
    int IMMERSIVE_MODE = 12;
    int DEAD_ZONE = 13;
    int SHAKE_THRESHOLD = 14;
    int GAMEPAD_RESET = 15;
}
