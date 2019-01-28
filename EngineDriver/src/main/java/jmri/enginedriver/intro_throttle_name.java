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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;



public class intro_throttle_name extends Fragment {

    SharedPreferences prefs;
    String currentValue = "";
    TextView throttleNameView;

    private threaded_application mainapp; //pointer back to application

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("Engine_Driver", "intro_throttle_name");

        super.onActivityCreated(savedInstanceState);
        mainapp = (threaded_application) this.getActivity().getApplication();
        prefs = this.getActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);
        currentValue = mainapp.fixThrottleName(prefs.getString("throttle_name_preference", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue)));

        throttleNameView = getView().findViewById(R.id.intro_throttle_name_value);
        throttleNameView.setText(currentValue);

        throttleNameView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    currentValue = mainapp.fixThrottleName(prefs.getString("throttle_name_preference", getActivity().getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue)));
                    throttleNameView.setText(currentValue);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_throttle_name, container, false);
    }


    @Nullable
    @Override
    public void onDestroyView() {
        prefs.edit().putString("throttle_name_preference", throttleNameView.getText().toString()).commit();
        super.onDestroyView();
    }
}
