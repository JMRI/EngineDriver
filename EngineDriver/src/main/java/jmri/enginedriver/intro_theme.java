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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Random;

import eu.esu.mobilecontrol2.sdk.MobileControl2;


public class intro_theme extends Fragment {

    private SharedPreferences prefs;
    private String currentValue = "";
    private String defaultName = "";
    private TextView v;
    private Spinner spinner;
    private int introThemeValueIndex = 1;
    private String[] nameEntries;
    private String[] nameEntryValues;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("Engine_Driver", "intro_theme");

        super.onActivityCreated(savedInstanceState);
        prefs = this.getActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);
        currentValue = prefs.getString("prefTheme", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));

        nameEntries = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThemeEntries);
        nameEntryValues = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThemeEntryValues);
        v = (RadioButton) getView().findViewById(R.id.intro_theme_default_name);
        v.setText(nameEntries[0]);
        v = (RadioButton) getView().findViewById(R.id.intro_theme_black_name);
        v.setText(nameEntries[1]);
        v = (RadioButton) getView().findViewById(R.id.intro_theme_outline_name);
        v.setText(nameEntries[2]);
        v = (RadioButton) getView().findViewById(R.id.intro_theme_ultra_name);
        v.setText(nameEntries[3]);
        v = (RadioButton) getView().findViewById(R.id.intro_theme_colorful_name);
        v.setText(nameEntries[4]);

        RadioGroup radioGroup = getView().findViewById(R.id.intro_throttle_type_radio_group);

        radioGroup.clearCheck();
        if (nameEntryValues[0].equals(currentValue)) {radioGroup.check(R.id.intro_theme_default_name); }
        else if (nameEntryValues[1].equals(currentValue)) {radioGroup.check(R.id.intro_theme_black_name); }
        else if (nameEntryValues[2].equals(currentValue)) {radioGroup.check(R.id.intro_theme_outline_name); }
        else if (nameEntryValues[3].equals(currentValue)) {radioGroup.check(R.id.intro_theme_ultra_name); }
        else if (nameEntryValues[4].equals(currentValue)) {radioGroup.check(R.id.intro_theme_colorful_name); }
        radioGroup.jumpDrawablesToCurrentState();

        radioGroup.setOnCheckedChangeListener(new
        RadioGroup.OnCheckedChangeListener() {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onCheckedChanged(RadioGroup group, int checkedId) {
              int Choice = 0;
              if (checkedId == R.id.intro_theme_default_name) { Choice = 0; }
              else if (checkedId == R.id.intro_theme_black_name) { Choice = 1; }
              else if (checkedId == R.id.intro_theme_outline_name) { Choice = 2; }
              else if (checkedId == R.id.intro_theme_ultra_name) { Choice = 3; }
              else if (checkedId == R.id.intro_theme_colorful_name) { Choice = 4; }
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
