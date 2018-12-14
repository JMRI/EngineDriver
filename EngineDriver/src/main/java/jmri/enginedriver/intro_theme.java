package jmri.enginedriver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Random;

import eu.esu.mobilecontrol2.sdk.MobileControl2;

/**
 * Created by andrew on 11/17/16.
 */

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
        super.onActivityCreated(savedInstanceState);
        prefs = this.getActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);
        currentValue = prefs.getString("prefTheme", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));

        nameEntries = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThemeEntries);
        nameEntryValues = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThemeEntryValues);
        v = getView().findViewById(R.id.intro_theme_default_name);
        v.setText(nameEntries[0]);
        v =  getView().findViewById(R.id.intro_theme_black_name);
        v.setText(nameEntries[1]);
        v = getView().findViewById(R.id.intro_theme_outline_name);
        v.setText(nameEntries[2]);
        v = getView().findViewById(R.id.intro_theme_ultra_name);
        v.setText(nameEntries[3]);
        v = getView().findViewById(R.id.intro_theme_colorful_name);
        v.setText(nameEntries[4]);

        spinner = (Spinner) getView().findViewById(R.id.intro_theme_value);
        spinner.setOnItemSelectedListener(new intro_theme.spinner_listener());

    }

    public class spinner_listener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            introThemeValueIndex = spinner.getSelectedItemPosition();
            prefs.edit().putString("prefTheme", nameEntryValues[introThemeValueIndex]).commit();  //save new name to prefs

//            InputMethodManager imm =
//                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            if ((imm != null) && (view != null)) {
//                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
//            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intro_theme, container, false);
    }

//    @Nullable
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//    }
}
