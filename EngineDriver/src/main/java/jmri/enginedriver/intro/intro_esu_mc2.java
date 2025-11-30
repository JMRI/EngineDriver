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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Objects;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

public class intro_esu_mc2 extends Fragment {
    static final String activityName = "intro_esu_mc2";

    private SharedPreferences prefs;
    private boolean esuMc2Yes;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(threaded_application.applicationName, activityName + ":");
        super.onActivityCreated(savedInstanceState);
        prefs = Objects.requireNonNull(this.getActivity()).getSharedPreferences("jmri.enginedriver_preferences", 0);

        RadioGroup radioGroup = Objects.requireNonNull(getView()).findViewById(R.id.intro_esu_mc2_radio_group);

        radioGroup.clearCheck();
        radioGroup.check(R.id.intro_esu_mc2_no);
        radioGroup.jumpDrawablesToCurrentState();

        radioGroup.setOnCheckedChangeListener(new
            RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.intro_esu_mc2_no) {
                    esuMc2Yes = false;
                    prefs.edit().putString("prefThrottleScreenType", "Default").commit();
                    prefs.edit().putString("NumThrottle", "2").commit();
                } else if (checkedId == R.id.intro_esu_mc2_yes) {
                    esuMc2Yes = true;
                }
                prefs.edit().putBoolean("display_speed_arrows_buttons", !esuMc2Yes).commit();
                prefs.edit().putBoolean("hide_slider_preference", esuMc2Yes).commit();
                prefs.edit().putBoolean("prefHideSliderAndSpeedButtons", esuMc2Yes).commit();
                prefs.edit().putBoolean("prefHideFunctionButtonsOfNonSelectedThrottle", esuMc2Yes).commit();
                prefs.edit().putBoolean("prefVolumeKeysFollowLastTouchedThrottleDefaultValue", esuMc2Yes).commit();
                prefs.edit().putBoolean("prefDoubleBackButtonToExit", esuMc2Yes).commit();
                prefs.edit().putBoolean("prefThrottleViewImmersiveMode", esuMc2Yes).commit();
         }
        });

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_esu_mc2, container, false);
    }

}
