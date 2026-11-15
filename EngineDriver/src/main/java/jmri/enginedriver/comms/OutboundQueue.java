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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Queue of messages waiting to be sent to the server, with a minimum interval between sends.
 * <p>
 * Messages are sent in the order they were queued. A newly queued message never overtakes
 * one that is already waiting, so an older value can never be sent after a newer one.
 * <p>
 * Speed messages are coalesced: when a speed message is queued for a throttle (WiThrottle)
 * or a cab (DCC-EX) that already has one waiting, the waiting one is removed and the new one
 * is added at the end. Only the latest speed is sent, and it is still sent after any message
 * (such as a direction change) that was queued before it.
 * <p>
 * Priority messages (function changes, whose timing matters) are not held back by the
 * interval; they are sent ahead of the waiting messages, in the order they were queued.
 * <p>
 * Time is passed in by the caller, which keeps this class free of Android dependencies.
 * All methods are thread safe.
 */
final class OutboundQueue {

    // WiThrottle speed for all locos on a throttle: M0A*<;>V25
    private static final Pattern WITHROTTLE_SPEED = Pattern.compile("^M(\\w)A\\*<;>V-?\\d+$");
    // DCC-EX throttle command, speed and direction for one cab: <t cab speed dir> or <t 0 cab speed dir>
    private static final Pattern DCCEX_THROTTLE = Pattern.compile("^<t (?:0 )?(\\d+) -?\\d+ [01]>$");

    private static final class Entry {
        final String message;
        final String coalesceKey;   // null if never coalesced

        Entry(String message, String coalesceKey) {
            this.message = message;
            this.coalesceKey = coalesceKey;
        }
    }

    private final ArrayDeque<Entry> priority = new ArrayDeque<>();
    private final ArrayDeque<Entry> normal = new ArrayDeque<>();
    private long lastSentMs;
    private boolean anySent = false;

    /**
     * @param message the message
     * @return the key under which this message replaces an older waiting one, or null
     */
    static String coalesceKey(String message) {
        Matcher m = WITHROTTLE_SPEED.matcher(message);
        if (m.matches()) return "witSpeed:" + m.group(1);
        m = DCCEX_THROTTLE.matcher(message);
        if (m.matches()) return "dccexThrottle:" + m.group(1);
        return null;
    }

    /** Queue a message, after all messages already waiting. */
    synchronized void add(String message) {
        String key = coalesceKey(message);
        if (key != null) {
            Iterator<Entry> it = normal.iterator();
            while (it.hasNext()) {
                if (key.equals(it.next().coalesceKey)) {
                    it.remove();
                    break;      // there is never more than one waiting per key
                }
            }
        }
        normal.addLast(new Entry(message, key));
    }

    /** Queue a message to be sent as soon as possible, ahead of the normal messages. */
    synchronized void addPriority(String message) {
        priority.addLast(new Entry(message, null));
    }

    /**
     * Take the next message that may be sent now, and record it as sent.
     *
     * @param nowMs      current time in milliseconds
     * @param intervalMs minimum interval between normal messages
     * @return the message, or null if none may be sent yet
     */
    synchronized String poll(long nowMs, long intervalMs) {
        Entry e = priority.pollFirst();
        if (e == null) {
            if (normal.isEmpty() || (anySent && (nowMs - lastSentMs < intervalMs))) return null;
            e = normal.pollFirst();
        }
        lastSentMs = nowMs;
        anySent = true;
        return e.message;
    }

    /**
     * @param nowMs      current time in milliseconds
     * @param intervalMs minimum interval between normal messages
     * @return milliseconds until the next message may be sent (0 if now), or -1 if nothing is waiting
     */
    synchronized long delayUntilNext(long nowMs, long intervalMs) {
        if (!priority.isEmpty()) return 0;
        if (normal.isEmpty()) return -1;
        if (!anySent) return 0;
        return Math.max(lastSentMs + intervalMs - nowMs, 0);
    }

    synchronized boolean isEmpty() {
        return priority.isEmpty() && normal.isEmpty();
    }

    synchronized int size() {
        return priority.size() + normal.size();
    }

    /** Discard all waiting messages. */
    synchronized void clear() {
        priority.clear();
        normal.clear();
    }
}
