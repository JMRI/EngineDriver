/*Copyright (C) 2013 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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
	private class ConLoco extends Loco {
		private boolean reversed;						//relative physical orientation wrt original lead (first entered) engine
		
		private ConLoco(String address) {
			super(address);
			reversed = false;
		}

		private ConLoco(Loco l) {
			super(l);
			reversed = false;
		}

		private boolean isReverse() {
			return reversed;
		}
		
		private void setReverse(boolean state) {
			reversed = state;
		}
		
	
	
	}

	private LinkedHashMap<String, ConLoco> con;			//locos assigned to this consist (i.e. this throttle)
	private String leadAddr;							//address of lead loco 
														//TODO: eliminate stored leadAddr and create on the fly?
	private String desc;								//composite of rosternames (or formatted addresses) of locos in consist
														//TODO: eliminate stored desc and create on the fly?
	

	public Consist() {
		con = new LinkedHashMap<String, ConLoco>();
		desc = "";
		leadAddr = "";
	}
	
	public Consist(Loco l) {
		this();
		this.add(l);
		leadAddr = l.getAddress();
	}
	
	public Consist(Consist c) {
		this();
		for(ConLoco l : c.con.values())
			this.add(l);
//		for(String addr : c.getList()) {						//loop through each engine in consist
//			this.add(c.getLoco(addr));							// and make deep copy of consist
//		}
		leadAddr = c.leadAddr;
	}
	
	//
	public void release() {
		con.clear();
	}
	
	public void add(String addr) {
		this.add(new ConLoco(addr));
	}
	
	public void add(Loco l) {
		this.add(new ConLoco(l));
	}
	
	public void add(ConLoco l) {
		String addr = l.getAddress();
		if(con.size() == 0)
			leadAddr = addr;
		con.put(l.getAddress(), new ConLoco(l));			//this ctor makes copy as objects are immutable
		desc = this.formatConsist();
	}
	
	public void remove(String address) {
		con.remove(address);
		desc = this.formatConsist();
	}
	
	public Loco getLoco(String address) {
		return con.get(address);
	}
	
	//
	// report direction of this engine relative to the current lead engine
	//
	public Boolean isReverse(String address) {
		ConLoco l = con.get(address);
		if(l == null)
			return null;
		boolean dir = l.isReverse();						//orientation of this loco
		boolean leadDir = con.get(leadAddr).isReverse();	//orientation of current lead loco
		return dir != leadDir;								//return true if orientation of htis loco is different from the lead
	}
	
	public void setReversed(String address) {
		setReversed(address, true);
	}
	
	public void setReversed(String address, boolean state) {
		ConLoco l = con.get(address);
		if(l != null)
			l.setReverse(state);
	}

	public Boolean isConfirmed(String address) {
		ConLoco l = con.get(address);
		return (l != null) ? l.isConfirmed() : null;
	}
	
	public void setConfirmed(String address) {
		setConfirmed(address, true);
	}
	
	public void setConfirmed(String address, boolean state) {
		ConLoco l = con.get(address);
		if(l != null)
			l.setConfirmed(state);
	}

	//get Set containing addresses of all locos in consist
	public Set<String> getList() {
		return con.keySet();
	}
	
	public boolean isEmpty() {
		return con.size() == 0;
	}
	
	public int size() {
		return con.size();
	}
	
	public String getLeadAddr() {
		return leadAddr;
	}
	
	public String setLeadAddr(String addr) {
		if(con.containsKey(addr))
			leadAddr = addr;
		return leadAddr;
	}
	
	//create string description of the consist
	@Override
	public String toString() {
		return desc;  
	}
	
	private String formatConsist() {
		String formatCon;
		if(con.size() > 0) {
			formatCon = "";
			String sep = "";
			for(Map.Entry<String, ConLoco> l : con.entrySet()) {		// loop through locos in consist
				formatCon += sep + l.getValue().toString();
				sep = " +";
			}
		}
		else {
			formatCon = "Not Set";
		}
		return formatCon;
	}
}
	