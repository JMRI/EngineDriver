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

package jmri.enginedriver.util;

/**
 * Model of the layout fast clock.
 * <p>
 * The server reports the fast time and the rate at which it runs (WiThrottle "PFT").
 * The server only reports again when the time or rate changes, so the throttle keeps
 * its own time in between: the current fast time is the reported time plus the real
 * time elapsed since the report, multiplied by the rate. A rate of 0 means the clock
 * is stopped.
 * <p>
 * Real time is passed in by the caller (a monotonic millisecond clock such as
 * {@code SystemClock.elapsedRealtime()}), which keeps this class free of Android
 * dependencies.
 */
public final class FastClock {
    private static final long MS_PER_MINUTE = 60_000L;

    private long referenceFastSeconds = 0L;
    private double rate = 0.0;
    private long referenceRealtimeMs = 0L;

    /**
     * Set the fast time as reported by the server.
     *
     * @param fastSeconds    fast time in seconds
     * @param newRate        fast clock rate; 0 (or less) means stopped
     * @param nowRealtimeMs  the caller's monotonic real time, in milliseconds
     */
    public synchronized void set(long fastSeconds, double newRate, long nowRealtimeMs) {
        referenceFastSeconds = fastSeconds;
        rate = (newRate > 0.0 && !Double.isInfinite(newRate) && !Double.isNaN(newRate)) ? newRate : 0.0;
        referenceRealtimeMs = nowRealtimeMs;
    }

    /** Clear the clock, for example when a new connection is made. */
    public synchronized void reset() {
        referenceFastSeconds = 0L;
        rate = 0.0;
        referenceRealtimeMs = 0L;
    }

    /** @return true if the clock is advancing */
    public synchronized boolean isRunning() {
        return rate > 0.0;
    }

    /** @return the fast clock rate, 0 if stopped */
    public synchronized double getRate() {
        return rate;
    }

    /**
     * @param nowRealtimeMs the caller's monotonic real time, in milliseconds
     * @return the current fast time in seconds
     */
    public synchronized long getSeconds(long nowRealtimeMs) {
        return getMillis(nowRealtimeMs) / 1000L;
    }

    /**
     * Real time until the displayed fast minute next changes.
     *
     * @param nowRealtimeMs the caller's monotonic real time, in milliseconds
     * @return delay in real milliseconds (at least 1), or -1 if the clock is stopped
     */
    public synchronized long getMillisUntilNextMinute(long nowRealtimeMs) {
        if (rate <= 0.0) return -1L;
        long fastMs = getMillis(nowRealtimeMs);
        long fastMsToNextMinute = MS_PER_MINUTE - Math.floorMod(fastMs, MS_PER_MINUTE);
        long realMs = (long) Math.ceil(fastMsToNextMinute / rate);
        return Math.max(realMs, 1L);
    }

    private long getMillis(long nowRealtimeMs) {
        long elapsedRealMs = Math.max(nowRealtimeMs - referenceRealtimeMs, 0L);
        return referenceFastSeconds * 1000L + (long) Math.floor(elapsedRealMs * rate);
    }
}
