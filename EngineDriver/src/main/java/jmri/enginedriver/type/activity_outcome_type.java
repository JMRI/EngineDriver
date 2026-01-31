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

import static android.app.Activity.RESULT_FIRST_USER;

public interface activity_outcome_type {
//    int RESULT_OK = -1;   // use the inbuilt value instead
//    int RESULT_CANCELED = 0; // use the inbuilt value instead
    int RESULT_LOCO_EDIT = RESULT_FIRST_USER + 1;
    int RESULT_CON_EDIT = RESULT_FIRST_USER + 2;
    int RESULT_CON_LIGHTS_EDIT = RESULT_FIRST_USER + 3;
    int RESULT_GAMEPAD = RESULT_FIRST_USER + 4;
    int RESULT_ESUMCII = RESULT_FIRST_USER + 5;
}
