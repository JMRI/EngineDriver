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

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

public class intro_write_settings extends Fragment {
    static final String activityName = "intro_write_settings";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(threaded_application.applicationName, activityName + ": onCreate()");
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onStart() {
        Log.d(threaded_application.applicationName, activityName + ": onStart()");
        super.onStart();
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        Log.d(threaded_application.applicationName, activityName + ":");
//        super.onActivityCreated(savedInstanceState);

        Button settingsButton = requireView().findViewById(R.id.intro_write_settings_launch);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                if (Build.VERSION.SDK_INT >= 23) {
                    intent.setAction(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                }
                Uri uri = Uri.fromParts("package", requireActivity().getApplicationContext().getPackageName(), null);
                intent.setData(uri);
                requireActivity().startActivity(intent);
            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_write_settings, container, false);
    }

}
