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

public interface alert_bundle_tag_type {
    String THROTTLE = "throt";
    String CONSIST = "con";
    String CONSIST_TEXT = "con_txt";
    String CONSIST_NAME = "con_name";
    String LOCO = "loco";
    String LOCO_TEXT = "loco_txt";
    String ROSTER_NAME = "roster_name";
    String SPEED = "speed";
    String FACING = "facing";
    String DIRECTION = "dir";
    String FUNCTION = "fn";
    String FUNCTION_ACTION = "fn_act";
    String TURNOUT = "turnout";
    String TURNOUT_ACTION = "turnout_act";
    String ROUTE = "route";
    String ROUTE_ACTION = "route_act";
    String FORCE = "force";
    String SPEED_STEPS = "steps";
    String COMMAND = "cmd";
    String MESSAGE = "msg";
    String POWER_STATE = "pwr";
    String TRACK = "trk";
    String TRACK_TYPE_TEXT= "trk_type_txt";
//    String TRACK_TEXT = "trk_txt";
    String TRACK_CHAR = "trk_char";
    String RESPONSE = "rsp";
    String SERVICE = "service";
    String ON_OFF = "on_off";

    String HOST_NAME = "host";
    String IP_ADDRESS = "ip";
    String PORT = "port";
    String SSID = "ssid";
    String SERVICE_TYPE = "service_type";

    String RESTART_REASON = "reason";
    String URGENT = "urgent";

    String CV = "cv";
    String CV_VALUE = "cv_val";
    String DECODER_ADDRESS = "addr";
    String CONSIST_ADDRESS = "con_addr";

    String LOG_ENTRY = "log_entry";

    String TIMER_TICK = "tick";

    // gamepads and keyboards
    String EVENT_ORIGIN = "event_origin";
    String EVENT_ACTION = "event_act";
    String EVENT_KEY_CODE = "key_code";
    String EVENT_IS_SHIFTED = "is_shifted";
    String EVENT_REPEAT_COUNT = "rpt_cnt";
    String EVENT_X_AXIS = "x";
    String EVENT_Y_AXIS = "y";
    String EVENT_X_AXIS_2 = "x2";
    String EVENT_Y_AXIS_2 = "y2";
}
