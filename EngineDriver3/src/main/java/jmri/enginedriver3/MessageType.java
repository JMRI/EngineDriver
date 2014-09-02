/* Copyright (C) 2014 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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

package jmri.enginedriver3;

//Constant values for Message types:  from->to (parms)
interface MessageType {
  //                                                   //from->to (parms)
  public static final int NONE=-1;
  public static final int ERROR=0;
  public static final int CONNECT_REQUESTED=1;  			//connect -> permafrag, which creates new websocket ()
  public static final int DISCONNECT_REQUESTED=2; 		//connect -> permafrag, which ends websocket nicely ()
//  public static final int FUNCTION_CHANGE_REQUESTED=6; 			//throttlefrag -> websocket (throttleKey, fn#, fnState)
  public static final int CONNECTED =7; 			//websocket -> all tabs ()
  public static final int DISCONNECTED =8; 			//websocket -> all tabs ()
  public static final int RELEASE_LOCO_REQUESTED=9; //from selectlocodialog->websocketthread (rostername, address, direction, fragmentname
  public static final int HEARTBEAT=12;  		//
  public static final int LOCO_REQUESTED=13; //from selectlocodialog->websocketthread (rostername, address, direction, fragmentname)
  public static final int TURNOUT_LIST_CHANGED =14;		//
  public static final int ROUTE_LIST_CHANGED =15;		//
  public static final int POWER_STATE_CHANGED=16;  	//
  public static final int SHUTDOWN=17;  		//permafrag -> all threads ()
  public static final int PANEL_LIST_CHANGED =18;		//
  public static final int THROTTLE_CHANGED=19; //websocket->throttlefrag (throttleKey)
  public static final int SPEED_CHANGE_REQUESTED =20; //throttlefrag -> websocket (throttleKey, desired displayedspeed)
  public static final int DIRECTION_CHANGE_REQUESTED=21; 	//throttlefrag -> websocket (throttleKey, desired direction)
  public static final int ROSTERENTRY_LIST_CHANGED=23;		//
  public static final int SEND_JSON_MESSAGE=24;		//  ->websocket (json string ready to send)
  //  public static final int LOCATION_DELIMITER=27;//
  public static final int JMRI_TIME_CHANGED =28;		//
  public static final int DISCOVERED_SERVER_LIST_CHANGED=30;		//
  public static final int MESSAGE_SHORT =31;		//any -> activity ()
  public static final int MESSAGE_LONG =32;		//any -> activity ()
  public static final int RAILROAD_CHANGED=33;		//
  public static final int LOOP=34;		//
  public static final int TURNOUT_CHANGE_REQUESTED=35;    //Turnouts->webSocket (systemName, newState)
  public static final int REMOVE_TAB_REQUESTED=36;		//removeTabDialog->activity (tabName)
  public static final int ADD_TAB_REQUESTED=37;		    //addTabDialog->activity (hashmap of settings for new tab)
}
