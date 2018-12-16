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
//    String defaultName = "";
    TextView v;

    private threaded_application mainapp; //pointer back to application

//    private String fixThrottleName(String currentValue) {
//        threaded_application mainapp;
//        defaultName = String.valueOf(R.string.prefThrottleNameDefaultValue);
//        mainapp = (threaded_application) this.getActivity().getApplication();
//
//        String newValue = currentValue;
//        //if name is blank or the default name, make it unique
//        if (currentValue.equals("") || currentValue.equals(defaultName)) {
//            Random rand = new Random();
//            String deviceId = String.valueOf(rand.nextInt(9999));  //use random string
//            if (MobileControl2.isMobileControl2()) {
//                // Change default name for ESU MCII
//                defaultName = this.getActivity().getApplicationContext().getResources().getString(R.string.prefEsuMc2ThrottleNameDefaultValue);
//            }
//            newValue = defaultName + " " + deviceId;
//        }
//        prefs.edit().putString("throttle_name_preference", newValue).commit();  //save new name to prefs
//        return newValue;
//    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainapp = (threaded_application) this.getActivity().getApplication();
        prefs = this.getActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);
        currentValue = mainapp.fixThrottleName(prefs.getString("throttle_name_preference", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue)));

        v = (TextView) getView().findViewById(R.id.intro_throttle_name_value);
        v.setText(currentValue);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_throttle_name, container, false);
    }


    @Nullable
    @Override
    public void onResume() {
        super.onResume();
        currentValue = mainapp.fixThrottleName(prefs.getString("throttle_name_preference", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue)));
        v.setText(currentValue);
    }

    @Nullable
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
