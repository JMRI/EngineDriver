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

public interface pref_gamepad_button_option_type {
    String ALL_STOP = "All Stop";
    String STOP = "Stop";
    String NEXT_THROTTLE = "Next Throttle";
    String FORWARD = "Forward";
    String REVERSE = "Reverse";
    String FORWARD_REVERSE_TOGGLE = "Forward/Reverse Toggle";
    String INCREASE_SPEED = "Increase Speed";
    String DECREASE_SPEED = "Decrease Speed";
    String LIMIT_SPEED = "Limit Speed";
    String PAUSE = "Pause";
    String SOUNDS_MUTE = "Mute (IPLS)";
    String SOUNDS_BELL = "Bell (IPLS)";
    String SOUNDS_HORN = "Horn (IPLS)";
    String SOUNDS_HORN_SHORT = "Horn Short (IPLS)";
    String SPEAK_CURRENT_SPEED = "Speak Current Speed";
    String NEUTRAL = "Neutral";
    String INCREASE_BRAKE = "Increase Brake";
    String DECREASE_BRAKE = "Decrease Brake";
    String INCREASE_LOAD = "Increase Load";
    String DECREASE_LOAD = "Decrease Load";
}
