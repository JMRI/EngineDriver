/*/* Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

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

package jmri.enginedriver;

public interface message_type {
    //Constant values for Message types:  from->to parms (action)
    int NONE = -1;
    int ERROR = 0;
    int CONNECT = 1;            // ca(select)->ta ipaddr, port (sends CONNECTED if no error)
    int DISCONNECT = 2;         // sl(exit)->ta  -- (send Q, turns off heartbeat, ends read timer)
    int REQ_LOCO_ADDR = 3;      // sl(acquire) -> ta engineaddr, size (sends Tengineaddr to WiT)
    int VELOCITY = 4;           // ed(sliderchg) -> ta speed (sends TVspeed to WiT)
    int DIRECTION = 5;          // ed(fwd/rev) -> ta direction (sends TRx to WiT)
    int FUNCTION = 6;           // ed(buttons) -> ta function, on then off (sends TFxy to WiT)
    int CONNECTED = 7;          // ta(connect success) -> ca -- (starts sl)
    int SERVICE_RESOLVED = 8;   // ta(service resolved) -> ca ipaddr, ,port  (adds to list)
    int RELEASE = 9;            // ed(exit) -> ta -- (sends Tr to WiT)
    int RESPONSE = 10;          // ta(msg from WiT) -> sl + ed msg (read changed variables)
//    int END_ACTIVITY = 11;      // not used
//    int HEARTBEAT = 12;         // ed(timer) -> ta  AND ta(receipt of heartbeat from ed) -> ed   strange, huh?
//    int LOCO_SELECTED = 13;     // ta(read msg) -> sl -- (start ed)
    int TURNOUT = 14;           // tu -> ta
    int ROUTE = 16;             // r -> ta
    int POWER_CONTROL = 15;     // pc -> ta
    int SHUTDOWN = 17;          // ca -> ta
    int SET_LISTENER = 18;      // ca -> ta, pass 1 to turn on, 0 to turn off
    int SERVICE_REMOVED = 19;   // ta(service removed) -> ca ipaddr, ,port  (removes from list)
//    int REQ_VEL_AND_DIR = 20;      // ed(delay after sliderchg) -> ta, pass whichThrottle, (requests velocity and direction from WiT)
//    int REQ_DIRECTION = 21;     // ed -> ta , pass whichThrottle, (requests direction from WiT)
    int WEBVIEW_LOC = 22;       // pref -> throt  user changed webview location pref
    int ROSTER_UPDATE = 23;     // ta -> ed  roster-related data updated in background
    int WIT_CON_RETRY = 24;     // ta -> ed  WiT connection lost and trying to reconnect
    int WIT_CON_RECONNECT = 25; // ta -> ed  WiT connection reestablished
    int INITIAL_WEB_WEBPAGE = 26;   // pref -> web  user changed initial webpage
    int INITIAL_THR_WEBPAGE = 27;   // pref -> throt   user changed initial webpage
    int TIME_CHANGED = 28;      // ta -> activities  updates current time
    int CLOCK_DISPLAY_CHANGED = 29;     // pref -> ta  clock display preference changed
    int ESTOP = 30;             // ta(sendeStopMsg) -> ta  estop requested
    int WIFI_QUIT = 31;   // ta(disconnect) -> ta send quit command to server
    int REQ_STEAL = 32;         // ta(message_action) pass addr, throttle -> throttle
    int STEAL = 33;             // ta(checkSteal) pass addr, throttle -> ta send commands to steal
    int WIFI_SEND = 34;         // ta, pass complete message, used for delayed sends
    int SEND_HEARTBEAT_START = 35;    // ta, pass complete message, used for delayed sends
    int FORCE_FUNCTION = 36;     // ed(buttons) -> ta function, on = 0 off =1(sends Tfxy to WiT)
    int TOAST_MESSAGE = 37;      //web_activity -> ta pass message text
    int KIDS_TIMER_ENABLE = 38;  //
    int KIDS_TIMER_START = 39;   //
    int KIDS_TIMER_END = 40;     //
    int KIDS_TIMER_TICK = 41;    //
    int IMPORT_SERVER_MANUAL_SUCCESS = 42;    //
    int IMPORT_SERVER_MANUAL_FAIL = 43;    //
    int IMPORT_SERVER_AUTO_AVAILABLE = 44;    //
    int WIT_TURNOUT_NOT_DEFINED = 45;    //
    int RESTART_APP = 46;   //
    int WIT_QUERY_SPEED_AND_DIRECTION = 47;
    int HTTP_SERVER_NAME_RECEIVED = 49;
    int CONNECTION_COMPLETED_CHECK = 50;
    int RELAUNCH_APP = 51;
    int SOUNDS_FORCE_LOCO_SOUNDS_TO_START = 52;
    int GAMEPAD_ACTION = 53;
    int GAMEPAD_JOYSTICK_ACTION = 54;
    int VOLUME_BUTTON_ACTION = 55;
    int REFRESH_FUNCTIONS = 56;
    int REQUEST_DECODER_ADDRESS = 57;
    int RECEIVED_DECODER_ADDRESS = 58;
    int WRITE_DECODER_ADDRESS = 59;
    int WRITE_DECODER_SUCCESS = 60;
    int WRITE_DECODER_FAIL = 61;
    int WRITE_CV = 62;
    int REQUEST_CV = 63;
    int RECEIVED_CV = 64;
    int DCCEX_RESPONSE = 65;
    int DCCEX_COMMAND_ECHO = 66;
    int DCCEX_SEND_COMMAND = 67;
    int WRITE_POM_CV = 68;
    int RECEIVED_TRACKS = 69;
    int REQUEST_TRACKS = 70;
    int WRITE_TRACK = 71;
    int WRITE_TRACK_POWER = 72;
    int REQUEST_REFRESH_THROTTLE = 73;
}
