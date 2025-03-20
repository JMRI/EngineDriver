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

public interface acceleratorometer_action_type {
    String NONE = "None";
    String NEXT_V = "NextV";
    String DIM_SCREEN = "Dim";
    String LOCK_DIM_SCREEN = "LockDim";
    String WEB_VIEW = "Web";
    String ALL_STOP = "AllStop";
    String E_STOP = "EStop";
}
