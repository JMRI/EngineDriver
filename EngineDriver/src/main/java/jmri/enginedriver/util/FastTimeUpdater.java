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

import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.message_type;

public class FastTimeUpdater implements Runnable {
    static final String activityName = "FastTimeUpdater";

    private final threaded_application mainapp;

    public FastTimeUpdater(threaded_application mainapp) {
        this.mainapp = mainapp;
    }

    @Override
    public void run() {

        double updatePeriod = threaded_application.FAST_TIME_INTERMEDIATE_UPDATE_PERIOD / 1000.0 / threaded_application.fastClockRatio;
        threaded_application.fastClockSeconds = threaded_application.fastClockSeconds + ((long) updatePeriod);

        threaded_application.logging(activityName + ": UpdateFastTime.run: :PFT: " + threaded_application.fastClockSeconds + " ratio: " + threaded_application.fastClockRatio + " UpdatePeriod: " + updatePeriod);

        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_TIME_CHANGE);

        threaded_application.fastTimeUpdateHandler.postDelayed(new FastTimeUpdater(mainapp), threaded_application.FAST_TIME_INTERMEDIATE_UPDATE_PERIOD);

    }

}
