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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.toolbar_button_size_to_use_type;
import jmri.enginedriver.type.toolbar_button_size_type;

public class intro_alps extends Fragment {
    static final String activityName = "intro_alps";

    private SharedPreferences prefs;
    private boolean alpsYes;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        threaded_application.logging(activityName + ": onCreate()");
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onStart() {
        threaded_application.logging(activityName + ": onStart()");
        super.onStart();
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        threaded_application.logging(activityName + ":");
//        super.onActivityCreated(savedInstanceState);

        prefs = this.requireActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);

        RadioGroup radioGroup = requireView().findViewById(R.id.intro_alps_radio_group);

        radioGroup.clearCheck();
        radioGroup.check(R.id.intro_esu_mc2_no);
        radioGroup.jumpDrawablesToCurrentState();

        radioGroup.setOnCheckedChangeListener(new
            RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
                if (checkedId == R.id.intro_esu_mc2_no) {
                    alpsYes = false;
                    prefs.edit().putString("prefThrottleScreenType", "Default").commit();
                    prefs.edit().putString("prefNumThrottles", "2").commit();
                } else if (checkedId == R.id.intro_esu_mc2_yes) {
                    alpsYes = true;
                }
                prefs.edit().putString("prefVolumeSpeedButtonsSpeedStep","4");
                prefs.edit().putString("prefToolbarButtonSize", toolbar_button_size_type.SMALL);
//                prefs.edit().putBoolean("prefDisplaySpeedButtons", !alpsYes).commit();
//                prefs.edit().putBoolean("prefHideSlider", alpsYes).commit();
//                prefs.edit().putBoolean("prefHideSliderAndSpeedButtons", alpsYes).commit();
//                prefs.edit().putBoolean("prefHideFunctionButtonsOfNonSelectedThrottle", alpsYes).commit();
//                prefs.edit().putBoolean("prefVolumeKeysFollowLastTouchedThrottleDefaultValue", alpsYes).commit();
//                prefs.edit().putBoolean("prefDoubleBackButtonToExit", alpsYes).commit();
//                prefs.edit().putBoolean("prefThrottleViewImmersiveMode", alpsYes).commit();
         }
        });

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_alps_page, container, false);
    }

}
