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

public interface sounds_type {
    int LOCO = 0;
    int BELL = 1;
    int HORN = 2;
    int HORN_SHORT = 3;

    int BUTTON_BELL = 0;
    int BUTTON_HORN = 1;
    int BUTTON_HORN_SHORT = 2;

    int BELL_HORN_START = 0;
    int BELL_HORN_LOOP = 1;
    int BELL_HORN_END = 2;

    int REPEAT_INFINITE = -1;
    int REPEAT_NONE = 0;

    int NOTHING_CURRENTLY_PLAYING = -1;
    int STARTUP_INDEX = 20;
    int SHUTDOWN_INDEX = 21;
}
