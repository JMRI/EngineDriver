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

public interface tick_type {
    int TICK_AUTO = -1;
    int TICK_0_2 = 2;
    int TICK_0_3 = 3;
    int TICK_0_4 = 4;
    int TICK_0_5 = 5;
    int TICK_0_6 = 6;
    int TICK_0_7 = 7;
    int TICK_0_8 = 8;
    int TICK_0_9 = 9;
    int TICK_0_10 = 10;
    int TICK_0_14 = 14;
    int TICK_0_28 = 28;
    int TICK_0_126 = 126;
    int TICK_0_100 = 100;

    // 1000 + the prefDisplaySpeedUnits
    int TICK_AUTO_0_AUTO = 999; // 1000 - 1
    int TICK_100_0_100 = 1100;
    int TICK_126_0_126 = 1126;
    int TICK_8_0_8 = 1008;
    int TICK_10_0_10 = 1010;
    int TICK_14_0_14 = 1014;
    int TICK_28_0_28 = 1028;

}
