package jmri.enginedriver;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by andrew on 11/17/16.
 */

public class intro_throttle_type extends Fragment {

    private SharedPreferences prefs;
    private String currentValue = "";
    private TextView v;
    private Spinner spinner;
    private int introThrottleTypeValueIndex = 1;
    private String[] nameEntries;
    private String[] nameEntryValues;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        prefs = this.getActivity().getSharedPreferences("jmri.enginedriver_preferences", 0);
        currentValue = prefs.getString("prefThrottleScreenType", this.getActivity().getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        nameEntries = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThrottleScreenTypeEntries);
        nameEntryValues = this.getActivity().getApplicationContext().getResources().getStringArray(R.array.prefThrottleScreenTypeEntryValues);
        v = getView().findViewById(R.id.intro_throttle_type_default_name);
        v.setText(nameEntries[0]);
        v =  getView().findViewById(R.id.intro_throttle_type_simple_name);
        v.setText(nameEntries[1]);
        v = getView().findViewById(R.id.intro_throttle_type_vertical_name);
        v.setText(nameEntries[2]);
        v = getView().findViewById(R.id.intro_throttle_type_big_left_name);
        v.setText(nameEntries[3]);


        spinner = (Spinner) getView().findViewById(R.id.intro_throttle_type_value);
        spinner.setOnItemSelectedListener(new intro_throttle_type.spinner_listener());

    }

    public class spinner_listener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            introThrottleTypeValueIndex = spinner.getSelectedItemPosition();
            prefs.edit().putString("prefThrottleScreenType", nameEntryValues[introThrottleTypeValueIndex]).commit();  //save new name to prefs

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
        return inflater.inflate(R.layout.intro_throttle_type, container, false);
    }

//    @Nullable
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//    }
}
