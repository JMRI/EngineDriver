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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

//
//EngineDriver simple Consist
//
//Consist just represents (one or more) Locos assigned to a throttle
//Locos in a Consist have a "reverse" property that indicates physical orientation with respect to the first loco (original lead loco) entered into the consist:
// set reverse to true if this loco faces in the opposite direction of the first loco entered
// reverse is always false for the lead loco
//
//
    public final class Consist {
    public static final int LIGHT_OFF = 0;
    public static final int LIGHT_FOLLOW = 1;
    public static final int LIGHT_UNKNOWN = 2;

    public class ConLoco extends Loco {
        private boolean backward;                        //end of loco that faces the top of the consist
        private int lightOn;                        //end of loco that faces the top of the consist

        private ConLoco(String address) {
            super(address);
            backward = false;
            lightOn = LIGHT_UNKNOWN;
        }

        private ConLoco(Loco l) {
            super( l);
            backward = false;
            lightOn = LIGHT_UNKNOWN;
        }

        public boolean isBackward() {
            return backward;
        }
        public int isLightOn() {
            return lightOn;
        }

    }

    private Map<String, ConLoco> con;                   //locos assigned to this consist (i.e. this throttle)
    private String leadAddr;                            //address of lead loco
    //TODO: eliminate stored leadAddr and create on the fly?
    private String trailAddr;                            //address of trail loco


    public Consist() {
        con = Collections.synchronizedMap(new LinkedHashMap<String, ConLoco>());
        leadAddr = "";
        trailAddr = "";
    }

    public Consist(Loco l) {
        this();

        this.add(l);
        leadAddr = l.getAddress();
        trailAddr = l.getAddress();
    }

    public Consist(Consist c) {
        this();
        for (ConLoco l : c.con.values()) {

            this.add(l);
        }
        leadAddr = c.leadAddr;
        trailAddr = c.leadAddr;
    }

    //
    public void release() {

        con.clear();
        leadAddr = "";
        trailAddr = "";
    }

    public void add(String addr) {
        this.add(new ConLoco(addr));
    }

    public void add(Loco l) {
        Loco nl = new Loco(l);
        this.add(new ConLoco(nl));
    }

    public void add(ConLoco l) {
        String addr = l.getAddress();
        if (!con.containsKey(addr)) {
            if (isEmpty()) {
                leadAddr = addr;
                trailAddr = addr;
            }
            con.put(addr, new ConLoco(l));                        //this ctor makes copy as objects are immutable

        }
    }

    public void remove(String address) {
        con.remove(address);
    }

    public ConLoco getLoco(String address) {
        return con.get(address);
    }

    //
    // report direction of this engine relative to the _current_ lead engine
    //
    // caller should catch null returned value indicating address is not in the consist
    //
    public Boolean isReverseOfLead(String address) {
        ConLoco l = con.get(address);
        if (l == null)
            return null;
        boolean dir = l.backward;                            //orientation of this loco
        boolean leadDir = con.get(leadAddr).backward;        //orientation of current lead loco
        return dir != leadDir;                                //return true if orientation of this loco is different from the lead
    }

    //
    // report direction of this engine relative to the top of the consist
    //
    // caller should catch null returned value indicating address is not in the consist
    //
    public Boolean isBackward(String address) {
        ConLoco l = con.get(address);
        if (l == null)
            return null;
        return l.backward;
    }

    public void setBackward(String address) {

        setBackward(address, true);
    }

    public void setBackward(String address, boolean state) {

        ConLoco l = con.get(address);
        if (l != null)
            l.backward = state;
    }

    public int isLight(String address) {
        ConLoco l = con.get(address);
        if (l == null)
            return LIGHT_UNKNOWN;
        return l.lightOn;
    }

    public void setLight(String address, int state) {

        ConLoco l = con.get(address);
        if (l != null)
            l.lightOn = state;
    }

    //
    // returns true if consist is not empty and the lead loco is from the roster
    public Boolean isLeadFromRoster() {
        boolean isRoster = false;
        if (!isEmpty() && leadAddr != null) {
            ConLoco l = con.get(leadAddr);
            if ((l != null) && (l.getIsFromRoster()))
                isRoster = true;
        }
        return isRoster;
    }

    //
    // returns true if consist is not empty and the lead loco has been confirmed
    public Boolean isActive() {
        boolean conGood = false;
        if (!isEmpty() && leadAddr != null) {
            ConLoco l = con.get(leadAddr);
            if (l != null && l.isConfirmed())
                conGood = true;
        }
        return conGood;
    }

    //
    // caller should catch null returned value indicating address is not in the consist
    //
    public Boolean isConfirmed(String address) {
        ConLoco l = con.get(address);
        return (l != null) ? l.isConfirmed() : null;
    }

    public void setConfirmed(String address) {
        setConfirmed(address, true);
    }

    public void setConfirmed(String address, boolean state) {
        ConLoco l = con.get(address);
        if (l != null)
            l.setConfirmed(state);
    }

    //get Set containing addresses of all locos in consist
    public Set<String> getList() {
        return con.keySet();
    }

    //get Set containing all locos in consist
    public Collection<ConLoco> getLocos() {
        return con.values();
    }

    public boolean isEmpty() {
        return con.size() == 0;
    }

    public boolean isMulti() {
        return (con.size() > 1 && isActive());
    }

    public int size() {
        return con.size();
    }

    public String getLeadAddr() {
        return leadAddr;
    }

    public String setLeadAddr(String addr) {
        if (con.containsKey(addr) && !leadAddr.equals(addr)) {
            leadAddr = addr;
        }
        return leadAddr;
    }

    public String getTrailAddr() {
        return trailAddr;
    }
    public String setTrailAddr(String addr) {
        if (con.containsKey(addr) && !trailAddr.equals(addr)) {
            trailAddr = addr;
        }
        return trailAddr;
    }

    //create string description of the consist
    @Override
    public String toString() {
        return formatConsist();
    }

    private String formatConsist() {
        StringBuilder formatCon;
        if (con.size() > 0) {
            formatCon = new StringBuilder();
            String sep = "";
            for (Map.Entry<String, ConLoco> l : con.entrySet()) {        // loop through locos in consist
                if (l.getValue().isConfirmed()) {
                    formatCon.append(sep).append(l.getValue().toString());
                    sep = " +";
                }
            }
        } else {
            formatCon = new StringBuilder("Not Set");
        }
        return formatCon.toString();
    }

    public String formatConsistAddr() {
        StringBuilder formatCon;
        if (con.size() > 0) {
            formatCon = new StringBuilder();
            String sep = "";
            for (Map.Entry<String, ConLoco> l : con.entrySet()) {        // loop through locos in consist
                if (l.getValue().isConfirmed()) {
                    formatCon.append(sep).append(l.getValue().getAddress().substring(1, l.getValue().getAddress().length()));
                    sep = ", ";
                }
            }
        } else {
            formatCon = new StringBuilder("Not Set");
        }
        return formatCon.toString();
    }

    public void setFunctionLabels(String address, String functionLabelsString,threaded_application mainapp) {
        ConLoco l = con.get(address);
        if (l != null)
            l.setFunctionLabels(functionLabelsString, mainapp);

    }
/*
    public String getFunctionLabel(String address, Integer functionNo) {
        String functionLabel = "";
        ConLoco l = con.get(address);
        if (l != null)
            functionLabel = l.getFunctionLabel(functionNo);
        return functionLabel;
    }
*/

}
