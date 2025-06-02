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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import java.util.Objects;

import jmri.enginedriver.R;

/**
 * Created by andrew on 11/17/16.
 */

public class intro_dccex extends Fragment {

    private SharedPreferences prefs;
    private boolean dccexYes;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("Engine_Driver", "intro_dccex");
        super.onActivityCreated(savedInstanceState);
        prefs = Objects.requireNonNull(this.getActivity()).getSharedPreferences("jmri.enginedriver_preferences", 0);
        boolean prefDccexConnectionOption = prefs.getBoolean("prefDCCEXconnectionOption", getResources().getBoolean(R.bool.prefDccexConnectionOptionDefaultValue));

//        TextView v = getView().findViewById(R.id.intro_dccex_no);
//        v.setText(this.getActivity().getApplicationContext().getResources().getString(R.string.introButtonsSlider));
//        v = getView().findViewById(R.id.intro_dccex_yes);
//        v.setText(this.getActivity().getApplicationContext().getResources().getString(R.string.introButtonsSliderAndButtons));

        RadioGroup radioGroup = Objects.requireNonNull(getView()).findViewById(R.id.intro_dccex_radio_group);

        radioGroup.clearCheck();
        if (!prefDccexConnectionOption) {radioGroup.check(R.id.intro_dccex_no); }
        else {radioGroup.check(R.id.intro_dccex_yes); }
        radioGroup.jumpDrawablesToCurrentState();

        radioGroup.setOnCheckedChangeListener(new
            RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.intro_dccex_no) {
                    dccexYes = false;
                    prefs.edit().putString("prefUseDccexProtocol","No").commit();
                } else if (checkedId == R.id.intro_dccex_yes) {
                    dccexYes = true;
                    prefs.edit().putString("prefUseDccexProtocol","Auto").commit();
                }
                prefs.edit().putBoolean("prefDCCEXconnectionOption", dccexYes).commit();
                prefs.edit().putBoolean("prefActionBarShowDccExButton", dccexYes).commit();
         }
        });

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_dccex, container, false);
    }

}
