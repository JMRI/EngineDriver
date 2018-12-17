package jmri.enginedriver;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Random;

import eu.esu.mobilecontrol2.sdk.MobileControl2;

/**
 * Created by andrew on 11/17/16.
 */

public class intro_throttle_name extends Fragment {

    SharedPreferences prefs;
    String currentValue = "";
    TextView throttleNameView;

    private threaded_application mainapp; //pointer back to application

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainapp = (threaded_application) this.getActivity().getApplication();
        prefs = this.getActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);
        currentValue = mainapp.fixThrottleName(prefs.getString("throttle_name_preference", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue)));

        throttleNameView = (TextView) getView().findViewById(R.id.intro_throttle_name_value);
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


//    @Nullable
//    @Override
//    public void onResume() {
//        super.onResume();
//        currentValue = mainapp.fixThrottleName(prefs.getString("throttle_name_preference", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue)));
//        throttleNameView.setText(currentValue);
//    }

    @Nullable
    @Override
    public void onDestroyView() {
        prefs.edit().putString("throttle_name_preference", throttleNameView.getText().toString()).commit();
        super.onDestroyView();
    }
}
