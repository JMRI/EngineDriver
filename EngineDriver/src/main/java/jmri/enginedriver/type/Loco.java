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

package jmri.enginedriver.type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import jmri.enginedriver.threaded_application;

//
// EngineDriver Loco
//
public class Loco {
    private static final int MAX_FUNCTIONS = 32;
    private final String addr;                  //L2531 form
    private final String formatAddr;            //2531(L) form
    private final String formatAddrHtml;            //2531(L) form with formatting
    //TODO: eliminate stored formatted address and create on the fly?
    private String desc;                        //typically the roster name
    private String rosterName;                  //null if loco has no roster entry
    private boolean confirmed;                  //set after WiT responds that engine is assigned to throttle
    private boolean isFromRoster;                  //true if the entry was found in the roster (for the function button label check)
    private boolean isServerSuppliedFunctionLabels;  //true if the the server supplied a function list from the loco (for the function button label check)
    private LinkedHashMap<Integer, String> functionLabels;
    private int functionLabelsMaxKey = 0;          // highest function number that contains a string (not null)

////    private static final String CONSIST_FUNCTION_ACTION_NONE = "none";
//    private static final String CONSIST_FUNCTION_ACTION_LEAD = "lead";
//    private static final String CONSIST_FUNCTION_ACTION_LEAD_AND_TRAIL = "lead and trail";
//    private static final String CONSIST_FUNCTION_ACTION_ALL = "all";
//    private static final String CONSIST_FUNCTION_ACTION_TRAIL = "trail";
//    private static final String CONSIST_FUNCTION_ACTION_LEAD_EXACT = "lead exact";
//    private static final String CONSIST_FUNCTION_ACTION_LEAD_AND_TRAIL_EXACT = "lead and trail exact";
//    private static final String CONSIST_FUNCTION_ACTION_ALL_EXACT = "all exact";
//    private static final String CONSIST_FUNCTION_ACTION_TRAIL_EXACT = "trail exact";
//    private static final String CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_LEAD = "f lead";
//    private static final String CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_LEAD_AND_TRAIL = "f lead and trail";
//    private static final String CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_ALL = "f all";
//    private static final String CONSIST_FUNCTION_ACTION_SAME_F_NUMBER_TRAIL = "f trail";

    public interface consist_function_action {
        String LEAD = "lead";
        String LEAD_AND_TRAIL = "lead and trail";
        String ALL = "all";
        String TRAIL = "trail";
        String LEAD_EXACT = "lead exact";
        String LEAD_AND_TRAIL_EXACT = "lead and trail exact";
        String ALL_EXACT = "all exact";
        String TRAIL_EXACT = "trail exact";
        String SAME_F_NUMBER_LEAD = "f lead";
        String SAME_F_NUMBER_LEAD_AND_TRAIL = "f lead and trail";
        String SAME_F_NUMBER_ALL = "f all";
        String SAME_F_NUMBER_TRAIL = "f trail";
    }

    public Loco(String address) {
        if (address != null)
            this.addr = address;
        else
            this.addr = "";
        this.formatAddr = formatAddress();
        this.formatAddrHtml = formatAddressHtml();
        this.desc = "";
        this.confirmed = false;
        this.rosterName = null;
        this.isFromRoster = false;
        this.isServerSuppliedFunctionLabels = false;
        this.functionLabels = null;
    }

    public Loco(Loco l) {
        this(l.addr);
        this.desc = l.desc;
        this.rosterName = l.rosterName;
        this.confirmed = l.confirmed;
        this.isFromRoster = l.isFromRoster;
        this.isServerSuppliedFunctionLabels = l.isServerSuppliedFunctionLabels;
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

    public Integer getIntAddress() {
        return Integer.parseInt(this.addr.substring(1));
    }

    public Integer getIntAddressLength() {
        String s = this.addr.substring(0,1);
        int rslt = 0;
        if (s.equals("L")) { rslt=1;}
        return rslt;
    }

    public String getFormatAddress() {
        return this.formatAddr;
    }

    public String getFormatAddressHtml() {
        return this.formatAddrHtml;
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
        if (rosterName == null) return;
        if (rosterName.trim().isEmpty()) return;  // don't accept ' ' (space or spaces) as a name
        this.rosterName = rosterName;
    }

    public boolean getIsFromRoster() {
        return this.isFromRoster;
    }
    public void setIsFromRoster(boolean isFromRoster) {
        this.isFromRoster = isFromRoster;
    }

    public boolean getIsServerSuppliedFunctionLabels() {
        return this.isServerSuppliedFunctionLabels;
    }
    public void setIsServerSuppliedFunctionLabels(boolean isFromRoster) {
        this.isServerSuppliedFunctionLabels = isFromRoster;
    }

    //provide description if present, otherwise provide formatted address
    @Override
    public String toString() {
        return (this.desc.length() > 0 ? this.desc : this.formatAddr);
    }

    private String formatAddress() {
        return this.addr.substring(1) + "(" + this.addr.charAt(0) + ")";  //reformat from L2591 to 2591(L)
    }

    public String toHtml() {
        return (this.desc.length() > 0 ? this.desc : this.formatAddrHtml);
    }

    private String formatAddressHtml() {
        return this.addr.substring(1) + "<small><small>(" + this.addr.charAt(0) + ")</small></small>";  //reformat from L2591 to 2591(L)
    }

    public void setFunctionLabels(String functionLabelsString) {
        String[] ta = threaded_application.splitByString(functionLabelsString, "]\\[");  //split into list of labels

        //populate a temp label array from RF command string
        functionLabels = new LinkedHashMap<>();
        int i = 0;
        for (String ts : ta) {
            if (i > 0 && !"".equals(ts)) { //skip first chunk, which is length, and skip any blank entries
                functionLabels.put(i - 1, ts); //index is hashmap key, value is label string
                functionLabelsMaxKey = i;
            }  //end if i>0
            i++;
        }  //end for
    }

    public void setFunctionLabels(LinkedHashMap<Integer, String> newFunctionLabels) {
        functionLabels = newFunctionLabels;
        if (functionLabels == null) {
            functionLabelsMaxKey = 0;
        } else {
            for(int i = MAX_FUNCTIONS; i >=0 ; i--) {
                if (functionLabels.get(i) != null) {
                    functionLabelsMaxKey = i;
                    break;
                }
            }
        }
    }

    public LinkedHashMap<Integer, String> getFunctionLabels() {
        return functionLabels;
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
        int functionNumber = -1;
        if (!lab.isEmpty()) {
             if (functionLabels != null) {
                 for (int i = 0; i <= functionLabelsMaxKey; i++) {
                     if (functionLabels.get(i) != null) {
                         if (functionLabels.get(i).equalsIgnoreCase(lab)) {
                             functionNumber = i;
                         }
                     }
                 }
             }
         }
         return functionNumber;
     }

    public List<Integer> getMatchingFunctions(Integer functionNumber, String searchLabel, boolean isLead, boolean isTrail,
                                              String prefConsistFollowRuleStyle, String prefConsistFollowDefaultAction,
                                              List<String> prefConsistFollowStrings, List<String> prefConsistFollowActions,
                                              List<Integer> prefConsistFollowHeadlights,
                                              threaded_application mainapp) {
        //List<String> functionList = new ArrayList<>();
        List<Integer> functionList = new ArrayList<>();
        int matchingRule = -1;

        if (prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.COMPLEX)) {
            // work out if/which rule the activated function matches
            // this is not searching the loco's labels
            for (int i = 0; i < prefConsistFollowStrings.size(); i++) {
//            if (searchLabel.toLowerCase().contains(prefConsistFollowStrings.get(i).toLowerCase())) {
                if (prefConsistFollowStrings.get(i).toLowerCase().contains(searchLabel.toLowerCase())) {
                    matchingRule = i;
                    //noinspection AssignmentToForLoopParameter
                    i = 999;
                    break;
                }
            }
            if (matchingRule >= 0) {
                // check if the loco matches the Action
                String Rule = prefConsistFollowActions.get(matchingRule);
                if (((Rule.equals(consist_function_action.LEAD))
                        && (isLead))
                        || ((Rule.equals(consist_function_action.LEAD_AND_TRAIL))
                        && ((isLead) || (isTrail)))
                        || (Rule.equals(consist_function_action.ALL))
                        || ((Rule.equals(consist_function_action.TRAIL))
                        && (isTrail))) {
                    // cycle through this locos function labels to find the partly matching string
                    if (functionLabels != null) {
                        for (int i = 0; i <= functionLabelsMaxKey; i++) {
                            if (functionLabels.get(i) != null) {
//                            if (functionLabels.get(i).toLowerCase().contains(prefConsistFollowStrings.get(matchingRule).toLowerCase())) {
                                if (prefConsistFollowStrings.get(matchingRule).toLowerCase().contains(functionLabels.get(i).toLowerCase())) {
                                    functionList.add(i);
                                }
                            }
                        }
                    }
                }

                // check the exact matching rules
                if (((Rule.equals(consist_function_action.LEAD_EXACT))
                        && (isLead))
                        || ((Rule.equals(consist_function_action.LEAD_AND_TRAIL_EXACT))
                        && ((isLead) || (isTrail)))
                        || (Rule.equals(consist_function_action.ALL_EXACT))
                        || ((Rule.equals(consist_function_action.TRAIL_EXACT))
                        && (isTrail))) {
                    // cycle through this locos function labels to find the exactly matching string
                    for (int i = 0; i <= functionLabelsMaxKey; i++) {
                        if (functionLabels != null) {
                            if (functionLabels.get(i) != null) {
                                if (functionLabels.get(i).equalsIgnoreCase(prefConsistFollowStrings.get(matchingRule))) {
                                    functionList.add(i);
                                }
                            }
                        }
                    }
                }
            }

            // if no matching rule was found, the default rule applies
            if (functionList.size() == 0) {
                if (((prefConsistFollowDefaultAction.equals(consist_function_action.SAME_F_NUMBER_LEAD))
                        && (isLead))
                        || ((prefConsistFollowDefaultAction.equals(consist_function_action.SAME_F_NUMBER_LEAD_AND_TRAIL))
                        && ((isLead) || (isTrail)))
                        || (prefConsistFollowDefaultAction.equals(consist_function_action.SAME_F_NUMBER_ALL))
                        || ((prefConsistFollowDefaultAction.equals(consist_function_action.SAME_F_NUMBER_TRAIL))
                        && (isTrail))) {
                    functionList.add(functionNumber);

                }
            }

        } else {   // Special - Partial or Exact

            String sl = searchLabel.toLowerCase().trim();
            if (functionLabels != null) {
                // cycle through this locos function labels to find the exactly matching string
                for (int i = 0; i <= functionLabelsMaxKey; i++) {
                    if (functionLabels.get(i) != null) {
                        boolean processThis = false;

                        if (mainapp.prefAlwaysUseDefaultFunctionLabels) {
                            if (mainapp.function_consist_locos.get(functionNumber).equals(consist_function_action.LEAD)) {
                                if (isLead) {
                                    processThis = true;
                                }
                            }
                            if (mainapp.function_consist_locos.get(functionNumber).equals(consist_function_action.LEAD_AND_TRAIL)) {
                                if ((isLead) || (isTrail)) {
                                    processThis = true;
                                }
                            }
                            if (mainapp.function_consist_locos.get(functionNumber).equals(consist_function_action.TRAIL)) {
                                if (isTrail) {
                                    processThis = true;
                                }
                            }
                            if (mainapp.function_consist_locos.get(functionNumber).equals(consist_function_action.ALL)) {
                                processThis = true;
                            }
                        } else {
                            processThis = true;
                        }

                        if (processThis) {
                            String fl = functionLabels.get(i).toLowerCase().trim();
                            if (prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.SPECIAL_EXACT)) {
                                if (fl.equals(sl)) {
                                    functionList.add(i);
                                }
                            } else {
                                if (prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.SPECIAL_PARTIAL)) {
                                    if ((fl.contains(sl)) || (sl.contains(fl))) {
                                        functionList.add(i);
                                    }
                                } else {
                                    if ((fl.contains(sl))) {
                                        functionList.add(i);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return functionList;
    }

}
    