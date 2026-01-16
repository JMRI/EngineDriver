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


public class intro_theme extends Fragment {
    static final String activityName = "intro_theme";

    private SharedPreferences prefs;
    private String[] nameEntryValues;

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
        String currentValue = prefs.getString("prefTheme", this.requireActivity().getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));

        //    private Spinner spinner;
        //    private int introThemeValueIndex = 1;
        String[] nameEntries = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThemeEntries);
        nameEntryValues = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThemeEntryValues);
        //    private String defaultName = "";
        TextView v = requireView().findViewById(R.id.intro_theme_default_name);
        v.setText(nameEntries[0]);
        v = requireView().findViewById(R.id.intro_theme_black_name);
        v.setText(nameEntries[1]);
        v = requireView().findViewById(R.id.intro_theme_outline_name);
        v.setText(nameEntries[2]);
        v = requireView().findViewById(R.id.intro_theme_ultra_name);
        v.setText(nameEntries[3]);
        v = requireView().findViewById(R.id.intro_theme_colorful_name);
        v.setText(nameEntries[4]);
        v = requireView().findViewById(R.id.intro_theme_neon_name);
        v.setText(nameEntries[5]);

        RadioGroup radioGroup = getView().findViewById(R.id.intro_throttle_type_radio_group);

        radioGroup.clearCheck();
        if (nameEntryValues[0].equals(currentValue)) {radioGroup.check(R.id.intro_theme_default_name); }
        else if (nameEntryValues[1].equals(currentValue)) {radioGroup.check(R.id.intro_theme_black_name); }
        else if (nameEntryValues[2].equals(currentValue)) {radioGroup.check(R.id.intro_theme_outline_name); }
        else if (nameEntryValues[3].equals(currentValue)) {radioGroup.check(R.id.intro_theme_ultra_name); }
        else if (nameEntryValues[4].equals(currentValue)) {radioGroup.check(R.id.intro_theme_colorful_name); }
        else if (nameEntryValues[5].equals(currentValue)) {radioGroup.check(R.id.intro_theme_neon_name); }
        radioGroup.jumpDrawablesToCurrentState();

        radioGroup.setOnCheckedChangeListener(new
        RadioGroup.OnCheckedChangeListener() {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
              int Choice;
              if (checkedId == R.id.intro_theme_default_name) { Choice = 0; }
              else if (checkedId == R.id.intro_theme_black_name) { Choice = 1; }
              else if (checkedId == R.id.intro_theme_outline_name) { Choice = 2; }
              else if (checkedId == R.id.intro_theme_ultra_name) { Choice = 3; }
              else if (checkedId == R.id.intro_theme_colorful_name) { Choice = 4; }
              else if (checkedId == R.id.intro_theme_neon_name) { Choice = 5; }
              else { Choice = 0;}

              prefs.edit().putString("prefTheme", nameEntryValues[Choice]).commit();
          }
        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_theme, container, false);
    }

}
