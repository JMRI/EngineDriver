/* Copyright (C) 2026 M. Steve Todd mstevetodd@gmail.com

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

import android.os.SystemClock;

import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.message_type;

/**
 * Tells the activities to redraw the fast clock each time the displayed minute changes.
 * <p>
 * The time itself is held by {@link FastClock}; this class only schedules the redraws,
 * on the main looper. Nothing is scheduled while the clock is stopped.
 */
public class FastTimeUpdater implements Runnable {
    static final String activityName = "FastTimeUpdater";

    private static final long MARGIN_MS = 20L;   // redraw just after the minute has changed

    private final threaded_application mainapp;

    private FastTimeUpdater(threaded_application mainapp) {
        this.mainapp = mainapp;
    }

    /**
     * (Re)start the redraws after the fast time or rate has changed.
     * Cancels any redraw already scheduled.
     */
    public static void start(threaded_application mainapp) {
        stop();
        schedule(mainapp);
    }

    /** Cancel any scheduled redraw. */
    public static void stop() {
        threaded_application.fastTimeUpdateHandler.removeCallbacksAndMessages(null);
    }

    private static void schedule(threaded_application mainapp) {
        long delayMs = threaded_application.fastClock.getMillisUntilNextMinute(SystemClock.elapsedRealtime());
        if (delayMs >= 0) {
            threaded_application.fastTimeUpdateHandler.postDelayed(new FastTimeUpdater(mainapp), delayMs + MARGIN_MS);
        }
    }

    @Override
    public void run() {
        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_TIME_CHANGE);
        schedule(mainapp);
    }
}
