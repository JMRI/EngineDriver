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

public interface heartbeat_interval_type {
    //all heartbeat values are in milliseconds
    int DEFAULT_OUTBOUND = 10000; //interval for outbound heartbeat when WiT heartbeat is disabled
    int MIN_OUTBOUND = 1000;   //minimum allowed interval for outbound heartbeat generator
    int MAX_OUTBOUND = 30000;  //maximum allowed interval for outbound heartbeat generator
    //    double HEARTBEAT_RESPONSE_FACTOR = 0.9;   //adjustment for inbound and outbound timers
    int MIN_INBOUND = 1000;   //minimum allowed interval for (enabled) inbound heartbeat generator
    int MAX_INBOUND = 60000;  //maximum allowed interval for inbound heartbeat generator
}
