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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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

    private static String CONSIST_FUNCTION_ACTION_NONE = "none";
    private static String CONSIST_FUNCTION_ACTION_LEAD = "lead";
    private static String CONSIST_FUNCTION_ACTION_LEAD_AND_TRAIL = "lead and trail";
    private static String CONSIST_FUNCTION_ACTION_ALL = "all";
    private static String CONSIST_FUNCTION_ACTION_TRAIL = "trail";
    private static String CONSIST_FUNCTION_ACTION_LEAD_EXACT = "lead exact";
    private static String CONSIST_FUNCTION_ACTION_LEAD_AND_TRAIL_EXACT = "lead and trail exact";
    private static String CONSIST_FUNCTION_ACTION_ALL_EXACT = "all exact";
    private static String CONSIST_FUNCTION_ACTION_TRAIL_EXACT = "trail exact";
    private static String CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_LEAD = "f lead";
    private static String CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_LEAD_AND_TRAIL = "f lead and trail";
    private static String CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_ALL = "f all";
    private static String CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_TRAIL = "f trail";

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
        this.functionLabels = null;
    }

    public Loco(Loco l) {
        this(l.addr);
        this.desc = l.desc;
        this.rosterName = l.rosterName;
        this.confirmed = l.confirmed;
        this.isFromRoster = l.isFromRoster;
        this.functionLabels = l.functionLabels;
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

    public void setFunctionLabels(String functionLabelsString,threaded_application mainapp) {
        String[] ta = mainapp.splitByString(functionLabelsString, "]\\[");  //split into list of labels

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

    public void setFunctionLabelDefaults(threaded_application mainapp, Integer whichThrottle) {
        if (mainapp.function_labels != null) {
            functionLabels = new LinkedHashMap<>(mainapp.function_labels_default);
        }
    }
/**
    public String getFunctionLabel(Integer functionNo) {
        String functionLabel = "";
        if (functionLabels != null) {
            functionLabel = functionLabels.get(functionNo);
            if (functionLabel == null)
                functionLabel = "";
        }
        return functionLabel;
    }
**/
     public Integer getFunctionNumberFromLabel(String lab) {
        Integer functionNumber = -1;
        if (!lab.equals("")) {
             if (functionLabels != null) {
                 for (int i = 0; i < functionLabels.size(); i++) {
                     if (functionLabels.get(i) != null) {
                         if (functionLabels.get(i).equals(lab)) {
                             functionNumber = i;
                         }
                     }
                 }
             }
         }
         return functionNumber;
     }

    public List<Integer> getMatchingFunctions(Integer functionNumber, String searchLabel, boolean isLead, boolean isTrail, String prefConsistFollowDefaultAction, List<String> prefConsistFollowStrings, List<String> prefConsistFollowActions, List<Integer> prefConsistFollowHeadlights) {
        //List<String> functionList = new ArrayList<>();
        List<Integer> functionList = new ArrayList<>();
        Integer matchingRule = -1;

        // work out if/which rule the activated function matches
        for(int i = 0; i < prefConsistFollowStrings.size(); i++) {
            if (searchLabel.toLowerCase().contains(prefConsistFollowStrings.get(i).toLowerCase())) {
                matchingRule = i;
                i = 999;
            }
        }
        if (matchingRule>=0) {
            // check if the loco matches the Action
            String Rule = prefConsistFollowActions.get(matchingRule);
            if ( ( ( Rule.equals(CONSIST_FUNCTION_ACTION_LEAD))
                    && (isLead) )
            || ( ( Rule.equals(CONSIST_FUNCTION_ACTION_LEAD_AND_TRAIL))
                    && ((isLead) || (isTrail)) )
            || ( Rule.equals(CONSIST_FUNCTION_ACTION_ALL))
            || ( ( Rule.equals(CONSIST_FUNCTION_ACTION_TRAIL))
                    && (isTrail) ) ) {
                    // cycle through this locos function labels to find the partly matching string
                if (functionLabels != null) {
                    for (int i = 0; i < functionLabels.size(); i++) {
                        if (functionLabels.get(i) != null) {
                            if (functionLabels.get(i).toLowerCase().contains(prefConsistFollowStrings.get(matchingRule).toLowerCase())) {
                                functionList.add(i);
                            }
                        }
                    }
                }
            }

            // check the exact matcing rules
            if ( ( ( Rule.equals(CONSIST_FUNCTION_ACTION_LEAD_EXACT))
                    && (isLead) )
            || ( ( Rule.equals(CONSIST_FUNCTION_ACTION_LEAD_AND_TRAIL_EXACT))
                    && ((isLead) || (isTrail)) )
            || ( Rule.equals(CONSIST_FUNCTION_ACTION_ALL_EXACT))
            || ( ( Rule.equals(CONSIST_FUNCTION_ACTION_TRAIL_EXACT))
                    && (isTrail) ) ) {
                // cycle through this locos function labels to find the exactly matching string
                for (int i = 0; i < functionLabels.size(); i++) {
                    if (functionLabels != null) {
                        if (functionLabels.get(i) != null) {
                            if (functionLabels.get(i).toLowerCase().equals(prefConsistFollowStrings.get(matchingRule).toLowerCase())) {
                                functionList.add(i);
                            }
                        }
                    }
                }
            }

            // if no matching rull was found, is if the default rule applies
            if (functionList.size()==0) {
                if ( ( ( Rule.equals(CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_LEAD))
                        && (isLead) )
                || ( ( Rule.equals(CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_LEAD_AND_TRAIL))
                        && ((isLead) || (isTrail)) )
                || ( Rule.equals(CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_ALL))
                || ( ( Rule.equals(CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_TRAIL))
                        && (isTrail) ) ) {
                    functionList.add(functionNumber);

                }
            }

        }
        return functionList;
    }

}
    