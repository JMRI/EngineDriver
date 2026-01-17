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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

/**
 * Created by andrew on 11/17/16.
 */

public class intro_buttons extends Fragment {
    static final String activityName = "intro_buttons";

    private SharedPreferences prefs;
    private boolean displaySpeedButtons = false;
    private boolean hideSlider = false;
    private boolean hideSliderAndButtons = false;

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

        prefs = this.requireActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);
        boolean prefDisplaySpeedButtons = prefs.getBoolean("prefDisplaySpeedButtons", false);
        boolean prefHideSlider = prefs.getBoolean("prefHideSlider", false);
        boolean prefHideSliderAndSpeedButtons = prefs.getBoolean("prefHideSliderAndSpeedButtons", false);

        TextView v = requireView().findViewById(R.id.intro_buttons_slider_name);
        v.setText(this.requireActivity().getApplicationContext().getResources().getString(R.string.introButtonsSlider));
        v = requireView().findViewById(R.id.intro_buttons_slider_and_buttons_name);
        v.setText(this.requireActivity().getApplicationContext().getResources().getString(R.string.introButtonsSliderAndButtons));
        v = requireView().findViewById(R.id.intro_buttons_no_slider_name);
        v.setText(this.requireActivity().getApplicationContext().getResources().getString(R.string.introButtonsNoSlider));



        RadioGroup radioGroup = requireView().findViewById(R.id.intro_buttons_radio_group);

        radioGroup.clearCheck();
        if (prefHideSliderAndSpeedButtons) { // overrides the other options
            radioGroup.check(R.id.intro_buttons_no_slider_or_buttons_name);
        } else if (!prefDisplaySpeedButtons && !prefHideSlider) {
            radioGroup.check(R.id.intro_buttons_slider_name);
        } else if (prefDisplaySpeedButtons && !prefHideSlider) {
            radioGroup.check(R.id.intro_buttons_slider_and_buttons_name);
        } else {
            radioGroup.check(R.id.intro_buttons_no_slider_name);
        }
        radioGroup.jumpDrawablesToCurrentState();

        radioGroup.setOnCheckedChangeListener(new
            RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
                if (checkedId == R.id.intro_buttons_no_slider_or_buttons_name) {
                    hideSliderAndButtons = true; displaySpeedButtons = false; hideSlider = false;
                } else if (checkedId == R.id.intro_buttons_slider_name) {
                    hideSliderAndButtons = false; displaySpeedButtons = false; hideSlider = false;
                } else if (checkedId == R.id.intro_buttons_slider_and_buttons_name) {
                    hideSliderAndButtons = false; displaySpeedButtons = true; hideSlider = false;
                } else if (checkedId == R.id.intro_buttons_no_slider_name) {
                    hideSliderAndButtons = false; displaySpeedButtons = true; hideSlider = true;
                }
                prefs.edit().putBoolean("prefDisplaySpeedButtons", displaySpeedButtons).commit();
                prefs.edit().putBoolean("prefHideSlider", hideSlider).commit();
                prefs.edit().putBoolean("prefHideSliderAndSpeedButtons", hideSliderAndButtons).commit();
         }
        });

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_buttons, container, false);
    }

}
