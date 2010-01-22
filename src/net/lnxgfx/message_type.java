/*Copyright (C) 2010 Jason M'Sadoques
  jlyonm@gmail.com

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

package net.lnxgfx;

interface message_type
{
  //Constant values for Message types:
  public static final int ERROR=0;
  public static final int CONNECT=1;
  public static final int DISCONNECT=2;
  public static final int LOCO_ADDR=3;
  public static final int VELOCITY=4;
  public static final int DIRECTION=5;
  public static final int FUNCTION=6;
  public static final int CONNECTED=7;
}
