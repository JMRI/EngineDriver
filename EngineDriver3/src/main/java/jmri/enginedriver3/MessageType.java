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

interface MessageType
{
    //Constant values for Message types:  from->to parms (action)
    public static final int NONE=-1;
    public static final int ERROR=0;
    public static final int CONNECT_REQUESTED=1;  			//
    public static final int DISCONNECT_REQUESTED=2; 		//
    //  public static final int LOCO_ADDR=3; 			//
//  public static final int VELOCITY=4; 			//
//  public static final int DIRECTION=5; 			//
//  public static final int FUNCTION=6; 			//
    public static final int CONNECTED =7; 			//
    public static final int DISCONNECTED =8; 			//
    //  public static final int RELEASE=9; 			//
//  public static final int RESPONSE=10;  		//
//  public static final int END_ACTIVITY=11;  	//
    public static final int HEARTBEAT=12;  		//
    //  public static final int LOCO_SELECTED=13;  	//
    public static final int TURNOUT_LIST_CHANGED =14;		//
    public static final int ROUTE_LIST_CHANGED =15;		//
    public static final int POWER_STATE_CHANGED=16;  	//
    public static final int SHUTDOWN=17;  		//
    //  public static final int SET_LISTENER=18;  	//
//  public static final int SERVICE_REMOVED=19; 	//
//  public static final int REQ_VELOCITY=20; 		//
//  public static final int REQ_DIRECTION=21; 	//
//  public static final int WEBVIEW_LOC=22;		//
    public static final int ROSTERENTRY_LIST_CHANGED=23;		//
//  public static final int WIT_CON_RETRY=24;		//
//  public static final int WIT_CON_RECONNECT=25;	//
//  public static final int INITIAL_WEBPAGE=26;	//
//  public static final int LOCATION_DELIMITER=27;//
    public static final int JMRI_TIME_CHANGED =28;		//
    //  public static final int CLOCK_DISPLAY=29;		//
    public static final int DISCOVERED_SERVER_LIST_CHANGED=30;		//
    public static final int MESSAGE_SHORT =31;		//
    public static final int MESSAGE_LONG =32;		//
    public static final int RAILROAD_CHANGED=33;		//
    public static final int LOOP=34;		//
    public static final int TURNOUT_CHANGE_REQUESTED=35;    //Turnouts->webSocket (systemName, newState)
    public static final int REMOVE_TAB_REQUESTED=36;		//removeTabDialog->activity (tabName)
    public static final int ADD_TAB_REQUESTED=37;		    //addTabDialog->activity (hashmap of settings for new tab)
}
