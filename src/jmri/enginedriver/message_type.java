/*/* Copyright (C) 2012 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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

interface message_type
{
  //Constant values for Message types:  from->to parms (action)
  public static final int NONE=-1;
  public static final int ERROR=0;
  public static final int CONNECT=1;  			// ca(select)->ta ipaddr, port (sends CONNECTED if no error)
  public static final int DISCONNECT=2; 		// sl(exit)->ta  -- (send Q, turns off heartbeat, ends read timer)
  public static final int LOCO_ADDR=3; 			// sl(acquire) -> ta engineaddr, size (sends Tengineaddr to WiT)
  public static final int VELOCITY=4; 				// ed(sliderchg) -> ta speed (sends TVspeed to WiT)
  public static final int DIRECTION=5; 			// ed(fwd/rev) -> ta direction (sends TRx to WiT)
  public static final int FUNCTION=6; 			// ed(buttons) -> ta function, on then off (sends TFxy to WiT)
  public static final int CONNECTED=7; 			// ta(connect success) -> ca -- (starts sl)
  public static final int SERVICE_RESOLVED=8; // ta(service resolved) -> ca ipaddr, ,port  (adds to list)
  public static final int RELEASE=9; 				// ed(exit) -> ta -- (sends TV0 and Tr to WiT)
  public static final int RESPONSE=10;  			// ta(msg from WiT) -> sl + ed msg (read changed variables)
  public static final int END_ACTIVITY=11;  		// not used
  public static final int HEARTBEAT=12;  		// ed(timer) -> ta  AND ta(receipt of heartbeat from ed) -> ed   strange, huh?
  public static final int LOCO_SELECTED=13;  	// ta(read msg) -> sl -- (start ed)
  public static final int TURNOUT=14;  	// tu -> ta
  public static final int ROUTE=16;  	// r -> ta
  public static final int POWER_CONTROL=15;  	// pc -> ta
  public static final int SHUTDOWN=17;  	// ca -> ta
  public static final int SET_LISTENER=18;  	// ca -> ta, pass 1 to turn on, 0 to turn off
  public static final int SERVICE_REMOVED=19; // ta(service removed) -> ca ipaddr, ,port  (removes from list)
  public static final int REQ_VELOCITY=20; 		// ed(delay after sliderchg) -> ta (requests velocity from WiT)
  public static final int REQ_DIRECTION=21; 	// ed -> ta (requests direction from WiT)
}
