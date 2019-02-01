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

package jmri.enginedriver;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by andrew on 11/17/16.
 */

public class intro_buttons extends Fragment {

    private SharedPreferences prefs;
    private TextView v;
    private boolean prefDisplaySpeedButtons;
    private boolean prefHideSlider;
    private boolean displaySpeedButtons = false;
    private boolean hideSlider = false;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("Engine_Driver", "intro_buttons");
        super.onActivityCreated(savedInstanceState);
        prefs = this.getActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);
        prefDisplaySpeedButtons = prefs.getBoolean("display_speed_arrows_buttons", false);
        prefHideSlider = prefs.getBoolean("hide_slider_preference", false);

        v = (RadioButton) getView().findViewById(R.id.intro_buttons_slider_name);
        v.setText(this.getActivity().getApplicationContext().getResources().getString(R.string.introButtonsSlider));
        v = (RadioButton) getView().findViewById(R.id.intro_buttons_slider_and_buttons_name);
        v.setText(this.getActivity().getApplicationContext().getResources().getString(R.string.introButtonsSliderAndButtons));
        v = (RadioButton) getView().findViewById(R.id.intro_buttons_no_slider_name);
        v.setText(this.getActivity().getApplicationContext().getResources().getString(R.string.introButtonsNoSlider));



        RadioGroup radioGroup = getView().findViewById(R.id.intro_buttons_radio_group);

        radioGroup.clearCheck();
        if (!prefDisplaySpeedButtons && !prefHideSlider) {radioGroup.check(R.id.intro_buttons_slider_name); }
        else if (prefDisplaySpeedButtons && !prefHideSlider) {radioGroup.check(R.id.intro_buttons_slider_and_buttons_name); }
        else {radioGroup.check(R.id.intro_buttons_no_slider_name); }
        radioGroup.jumpDrawablesToCurrentState();

        radioGroup.setOnCheckedChangeListener(new
            RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.intro_buttons_slider_name) { displaySpeedButtons = false; hideSlider = false; }
                else if (checkedId == R.id.intro_buttons_slider_and_buttons_name) {  displaySpeedButtons = true; hideSlider = false;  }
                else if (checkedId == R.id.intro_buttons_no_slider_name) {  displaySpeedButtons = true; hideSlider = true;  }
                prefs.edit().putBoolean("display_speed_arrows_buttons", displaySpeedButtons).commit();
                prefs.edit().putBoolean("hide_slider_preference", hideSlider).commit();
         }
        });

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_buttons, container, false);
    }

}
