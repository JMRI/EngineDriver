/*Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

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

import java.util.LinkedHashMap;

//
// EngineDriver Loco
//
public class Loco {
    private final String addr;                  //L2531 form
    private final String formatAddr;            //2531(L) form
    //TODO: eliminate stored formatted address and create on the fly?
    private String desc;                        //typically the roster name
    private String rosterName;                  //null if loco has no roster entry
    private boolean confirmed;                  //set after WiT responds that engine is assigned to throttle
    private boolean isFromRoster;                  //true if the entry was found in the roster (for the function button label check)
    private LinkedHashMap<Integer, String> functionLabels;

    public Loco(String address) {
        if (address != null)
            this.addr = address;
        else
            this.addr = "";
        this.formatAddr = formatAddress();
        this.desc = "";
        this.confirmed = false;
        this.rosterName = null;
        this.isFromRoster = false;
    }

    public Loco(Loco l) {
        this(l.addr);
        this.desc = l.desc;
        this.rosterName = l.rosterName;
        this.confirmed = l.confirmed;
        this.isFromRoster = l.isFromRoster;
    }

    public boolean isConfirmed() {
        return this.confirmed;
    }

    public void setConfirmed() {
        setConfirmed(true);
    }

    public void setConfirmed(boolean state) {
        this.confirmed = state;
    }

    public String getAddress() {
        return this.addr;
    }

    public String getFormatAddress() {
        return this.formatAddr;
    }

    public void setDesc(String rosterName) {
        this.desc = rosterName;
    }

    public String getDesc() {
        return this.desc;
    }
    public String getRosterName() {
        return this.rosterName;
    }
    public void setRosterName(String rosterName) {
        this.rosterName = rosterName;
    }

    public boolean getIsFromRoster() {
        return this.isFromRoster;
    }
    public void setIsFromRoster(boolean isFromRoster) {
        this.isFromRoster = isFromRoster;
    }

    //provide description if present, otherwise provide formatted address
    @Override
    public String toString() {
        return (this.desc.length() > 0 ? this.desc : this.formatAddr);
    }

    private String formatAddress() {
        return this.addr.substring(1) + "(" + this.addr.substring(0, 1) + ")";  //reformat from L2591 to 2591(L)
    }

    public void setFunctionLabels(String functionLabelsString) {
        String[] ta = splitByString(functionLabelsString, "]\\[");  //split into list of labels

        //populate a temp label array from RF command string
        functionLabels = new LinkedHashMap<>();
        int i = 0;
        for (String ts : ta) {
            if (i > 0 && !"".equals(ts)) { //skip first chunk, which is length, and skip any blank entries
                functionLabels.put(i - 1, ts); //index is hashmap key, value is label string
            }  //end if i>0
            i++;
        }  //end for
    }

    static private String[] splitByString(String input, String divider) {

        //bail on empty input string, return input as single element
        if (input == null || input.length() == 0) return new String[]{input};

        int size = 0;
        String temp = input;

        // count entries
        while (temp.length() > 0) {
            size++;
            int index = temp.indexOf(divider);
            if (index < 0) break;    // break not found
            temp = temp.substring(index + divider.length());
            if (temp.length() == 0) {  // found at end
                size++;
                break;
            }
        }

        String[] result = new String[size];

        // find entries
        temp = input;
        size = 0;
        while (temp.length() > 0) {
            int index = temp.indexOf(divider);
            if (index < 0) break;    // done with all but last
            result[size] = temp.substring(0, index);
            temp = temp.substring(index + divider.length());
            size++;
        }
        result[size] = temp;

        return result;
    }

}
    