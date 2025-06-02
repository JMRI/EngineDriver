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
import android.widget.TextView;

import java.util.Objects;

import jmri.enginedriver.R;

public class intro_throttle_type extends Fragment {

    private SharedPreferences prefs;
    private String[] nameEntryValues;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("Engine_Driver", "intro_throttle_type");
        super.onActivityCreated(savedInstanceState);
        prefs = Objects.requireNonNull(this.getActivity()).getSharedPreferences("jmri.enginedriver_preferences", 0);
        String currentValue = prefs.getString("prefThrottleScreenType", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        //    private Spinner spinner;
        //    private int introThrottleTypeValueIndex = 1;
        String[] nameEntries = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThrottleScreenTypeEntries);
        nameEntryValues = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThrottleScreenTypeEntryValues);
        TextView v = Objects.requireNonNull(getView()).findViewById(R.id.intro_throttle_type_default_name);
        v.setText(nameEntries[0]);
        v = getView().findViewById(R.id.intro_throttle_type_vertical_name);
        v.setText(nameEntries[1]);
        v = getView().findViewById(R.id.intro_throttle_type_big_left_name);
        v.setText(nameEntries[2]);
        v = getView().findViewById(R.id.intro_throttle_type_big_right_name);
        v.setText(nameEntries[3]);
        v = getView().findViewById(R.id.intro_throttle_type_vertical_left_name);
        v.setText(nameEntries[4]);
        v = getView().findViewById(R.id.intro_throttle_type_vertical_right_name);
        v.setText(nameEntries[5]);
        v = getView().findViewById(R.id.intro_throttle_type_switching_name);
        v.setText(nameEntries[6]);
        v = getView().findViewById(R.id.intro_throttle_type_switching_left_name);
        v.setText(nameEntries[7]);
        v = getView().findViewById(R.id.intro_throttle_type_switching_right_name);
        v.setText(nameEntries[8]);
        v = getView().findViewById(R.id.intro_throttle_type_switching_horizontal_name);
        v.setText(nameEntries[9]);
        v = getView().findViewById(R.id.intro_throttle_type_simple_name);
        v.setText(nameEntries[10]);
        v = getView().findViewById(R.id.intro_throttle_type_tablet_switching_left_name);
        v.setText(nameEntries[11]);


        RadioGroup radioGroup = getView().findViewById(R.id.intro_throttle_type_radio_group);

        radioGroup.clearCheck();
        if (nameEntryValues[0].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_default_name); }
        else if (nameEntryValues[1].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_vertical_name); }
        else if (nameEntryValues[2].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_big_left_name); }
        else if (nameEntryValues[3].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_big_right_name); }
        else if (nameEntryValues[4].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_vertical_left_name); }
        else if (nameEntryValues[5].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_vertical_right_name); }
        else if (nameEntryValues[6].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_switching_name); }
        else if (nameEntryValues[7].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_switching_left_name); }
        else if (nameEntryValues[8].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_switching_right_name); }
        else if (nameEntryValues[9].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_switching_horizontal_name); }
        else if (nameEntryValues[10].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_simple_name); }
        else if (nameEntryValues[11].equals(currentValue)) {radioGroup.check(R.id.intro_throttle_type_tablet_switching_left_name); }
        radioGroup.jumpDrawablesToCurrentState();

        radioGroup.setOnCheckedChangeListener(new
            RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int Choice;
                if (checkedId == R.id.intro_throttle_type_default_name) { Choice = 0; }
                else if (checkedId == R.id.intro_throttle_type_vertical_name) { Choice = 1; }
                else if (checkedId == R.id.intro_throttle_type_big_left_name) { Choice = 2; }
                else if (checkedId == R.id.intro_throttle_type_big_right_name) { Choice = 3; }
                else if (checkedId == R.id.intro_throttle_type_vertical_left_name) { Choice = 4; }
                else if (checkedId == R.id.intro_throttle_type_vertical_right_name) { Choice = 5; }
                else if (checkedId == R.id.intro_throttle_type_switching_name) { Choice = 6; }
                else if (checkedId == R.id.intro_throttle_type_switching_left_name) { Choice = 7; }
                else if (checkedId == R.id.intro_throttle_type_switching_right_name) { Choice = 8; }
                else if (checkedId == R.id.intro_throttle_type_switching_horizontal_name) { Choice = 9; }
                else if (checkedId == R.id.intro_throttle_type_simple_name) { Choice = 10; }
                else if (checkedId == R.id.intro_throttle_type_tablet_switching_left_name) { Choice = 11; }
                else { Choice = 0; }

                prefs.edit().putString("prefThrottleScreenType", nameEntryValues[Choice]).commit();
         }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_throttle_type, container, false);
    }

}
