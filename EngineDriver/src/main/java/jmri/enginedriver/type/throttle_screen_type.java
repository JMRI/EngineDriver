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

public interface throttle_screen_type {
    String DEFAULT                  = "Default";
    String VERTICAL                 = "Vertical";
    String BIG_LEFT                 = "Big Left";
    String BIG_RIGHT                = "Big Right";
    String VERTICAL_LEFT            = "Vertical Left";
    String VERTICAL_RIGHT           = "Vertical Right";
    String SWITCHING                = "Switching";
    String SWITCHING_LEFT           = "Switching Left";
    String SWITCHING_RIGHT          = "Switching Right";
    String SWITCHING_HORIZONTAL     = "Switching Horizontal";
    String SIMPLE                   = "Simple";
    String TABLET_SWITCHING_LEFT    = "Tablet Switching Left";
    String TABLET_VERTICAL_LEFT     = "Tablet Vertical Left";
    String SEMI_REALISTIC_LEFT      = "Semi Realistic Left";

    String CONTAINS_VERTICAL        = "Vertical";
    String CONTAINS_SWITCHING       = "Switching";
    String CONTAINS_SEMI_REALISTIC  = "Semi Realistic";
}

