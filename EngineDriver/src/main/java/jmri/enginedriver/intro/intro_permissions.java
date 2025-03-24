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

Derived from the samples for AppIntro at https://github.com/paolorotolo/AppIntro

*/

package jmri.enginedriver.intro;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Objects;

import jmri.enginedriver.R;
import jmri.enginedriver.util.PermissionsHelper;

/**
 * Created by andrew on 11/17/16.
 */

public class intro_permissions extends Fragment {

    private boolean askedThisSession = false;
    int permissionId;
    String permissionLabel;
    TextView introPermissionLabel;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("Engine_Driver", "intro_permissions");
        super.onActivityCreated(savedInstanceState);

        introPermissionLabel = getView().findViewById(R.id.intro_permission_label);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            permissionId = Integer.parseInt(Objects.requireNonNull(getArguments().getString("id")));
            permissionLabel = getArguments().getString("label");
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            // User is viewing the fragment,
            // or fragment is inside the screen
            if (!askedThisSession) {
                introPermissionLabel.setText(permissionLabel);
                PermissionsHelper phi = PermissionsHelper.getInstance();
                phi.setIsDialogOpen(false);
                if (!phi.isPermissionGranted(this.getActivity(), permissionId)) {
                    ActivityCompat.shouldShowRequestPermissionRationale(this.getActivity(), PermissionsHelper.getManifestPermissionId(permissionId));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        phi.requestNecessaryPermissions(this.getActivity(), permissionId);
                    }
                }
            }
            askedThisSession = true;
        }
        else {
            // User is not viewing the fragment,
            // or fragment is out of the screen
            askedThisSession = false;
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_permissions, container, false);
    }

}
