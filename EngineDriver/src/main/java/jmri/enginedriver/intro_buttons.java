package jmri.enginedriver;

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

        if (!prefDisplaySpeedButtons && !prefHideSlider) {radioGroup.check(R.id.intro_buttons_slider_name); }
        else if (prefDisplaySpeedButtons && !prefHideSlider) {radioGroup.check(R.id.intro_buttons_slider_and_buttons_name); }
        else {radioGroup.check(R.id.intro_buttons_no_slider_name); }

        radioGroup.setOnCheckedChangeListener(new
            RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int Choice = 0;
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
