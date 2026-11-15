/* Copyright (C) 2026 Keith Bruce

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

package jmri.enginedriver.comms;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text handling for DCC-EX responses, on the path every received line takes.
 * <p>
 * Kept free of Android dependencies.
 */
final class DccexText {

    // anything inside double quotes, allowing for escaped quotes \"
    private static final Pattern QUOTED = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");

    /** Same limit as the String.split() call this replaces. */
    static final int MAX_ARGS = 999;

    private DccexText() {
    }

    /**
     * Replace '&lt;' and '&gt;' inside double-quoted text with '‹' and '›', so that they
     * cannot be taken for the start or end of a command.
     */
    static String replaceAnglesInQuotes(String input) {
        if (input.indexOf('"') < 0) return input;      // nothing quoted: nothing to replace
        Matcher matcher = QUOTED.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String replaced = matcher.group().replace("<", "‹").replace(">", "›");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replaced));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Split the body of a DCC-EX response into its arguments at the spaces that are outside
     * double quotes. Gives the same result as
     * {@code body.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 999)}: a space is a separator when
     * an even number of double quotes follows it. That regular expression scans the rest of the
     * line at every space, which takes time proportional to the square of the line length;
     * this takes linear time.
     */
    static String[] splitArgs(String body) {
        int quotesRemaining = 0;
        for (int i = 0; i < body.length(); i++) {
            if (body.charAt(i) == '"') quotesRemaining++;
        }
        ArrayList<String> args = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"') {
                quotesRemaining--;
            } else if ((c == ' ') && ((quotesRemaining % 2) == 0) && (args.size() < MAX_ARGS - 1)) {
                args.add(body.substring(start, i));
                start = i + 1;
            }
        }
        args.add(body.substring(start));
        return args.toArray(new String[0]);
    }
}
