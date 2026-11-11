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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import jmri.enginedriver.threaded_application;

/**
 * Selects the local (Wi-Fi or Ethernet) network for the app's connections.
 * <p>
 * A command station access point usually has no internet access, so Android keeps mobile
 * data as its default network while the phone is also joined to the access point. Unless
 * mobile data is allowed in the preferences, the app binds its process to the local network
 * before connecting, so that the throttle connection (and the web and roster connections to
 * the same server) go over Wi-Fi. This is done synchronously, immediately before a
 * connection is made, so the first connection attempt is already on the local network and a
 * change of Wi-Fi network is picked up on the next connection.
 */
public final class LocalNetwork {
    static final String activityName = "LocalNetwork";

    private LocalNetwork() {
    }

    /**
     * @param context any context
     * @return a connected Wi-Fi or Ethernet network, or null if there is none
     */
    @SuppressWarnings("deprecation") // NetworkInfo: still the way to check that a network is connected on API 21-22
    public static Network find(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;
        for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if ((caps == null)
                    || !(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
                continue;
            }
            NetworkInfo info = cm.getNetworkInfo(network);
            if ((info != null) && info.isConnected()) {
                return network;
            }
        }
        return null;
    }

    /**
     * Bind the process to the local network, or, if mobile data is allowed, release any binding
     * so that the system's default network is used.
     *
     * @param context          any context
     * @param allowMobileData  the "allow mobile data" preference
     * @return true if a local network was found (or mobile data is allowed)
     */
    @SuppressWarnings("deprecation") // setProcessDefaultNetwork: the API 21-22 equivalent of bindProcessToNetwork
    public static boolean select(Context context, boolean allowMobileData) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return allowMobileData;
        Network network = allowMobileData ? null : find(context);
        if (Build.VERSION.SDK_INT >= 23) {
            cm.bindProcessToNetwork(network);
        } else {
            ConnectivityManager.setProcessDefaultNetwork(network);
        }
        if (!allowMobileData) {
            threaded_application.logging(activityName + ": select(): "
                    + ((network != null) ? "bound to local network " + network : "no local network found"));
        }
        return allowMobileData || (network != null);
    }
}
