package jmri.enginedriver.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

public class cvBitCalculator extends DialogFragment {
    static final String activityName = "cvBitCalculator";

    private EditText editText;
    private CheckBox[] checkBoxes = new CheckBox[8];
    private Button buttonConfirm;
    private Button buttonClose;

    private int initialCv;
    private TextView messageText;
    private ScrollView messageTextScrollView;

    // To prevent feedback loops between EditText and CheckBoxes
    private boolean isUpdatingFromEditText = false;
    private boolean isUpdatingFromCheckBox = false;

    // Interface to communicate back to the Activity/Fragment
    public interface OnConfirmListener {
        void onConfirm(String inputText, List<Boolean> checkboxStates);
    }

    private OnConfirmListener listener;

    public static cvBitCalculator newInstance(int initialValue, int initialCv, int themeResId) {
        cvBitCalculator fragment = new cvBitCalculator();
        Bundle args = new Bundle();
        args.putInt("initialValue", initialValue); // Use a key to store the value
        args.putInt("initialCv", initialCv); // Use a key to store the value
        args.putInt("themeResId", themeResId); // Store the theme
        fragment.setArguments(args);
        return fragment;
    }

    // Call this method to set the listener from the hosting Activity/Fragment
    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int themeResId = getArguments().getInt("themeResId");
            if (themeResId != 0) {
                setStyle(DialogFragment.STYLE_NORMAL, themeResId);
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        int themeResId = 0;
        if (getArguments() != null) {
            themeResId = getArguments().getInt("themeResId");
        }

        Context themedContext = requireActivity();
        if (themeResId != 0) {
            themedContext = new androidx.appcompat.view.ContextThemeWrapper(requireActivity(), themeResId);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.cv_bit_calculator_dialog, null); // Use your layout file name

        checkBoxes[0] = view.findViewById(R.id.cv_bit_calculator_bit_0);
        checkBoxes[1] = view.findViewById(R.id.cv_bit_calculator_bit_1);
        checkBoxes[2] = view.findViewById(R.id.cv_bit_calculator_bit_2);
        checkBoxes[3] = view.findViewById(R.id.cv_bit_calculator_bit_3);
        checkBoxes[4] = view.findViewById(R.id.cv_bit_calculator_bit_4);
        checkBoxes[5] = view.findViewById(R.id.cv_bit_calculator_bit_5);
        checkBoxes[6] = view.findViewById(R.id.cv_bit_calculator_bit_6);
        checkBoxes[7] = view.findViewById(R.id.cv_bit_calculator_bit_7);

        editText = view.findViewById(R.id.cv_bit_calculator_edit_text_input);
        messageTextScrollView = view.findViewById(R.id.cv_bit_calculator_message_scroll_view);
        messageText = view.findViewById(R.id.cv_bit_calculator_message);

        // Retrieve the initial value from arguments
        if (getArguments() != null) {
            int initialValue = getArguments().getInt("initialValue", 0); // Default to 0 if not found
            editText.setText(String.valueOf(initialValue));
            initialCv = getArguments().getInt("initialCv", 0); // Default to 0 if not found
            isUpdatingFromEditText = true;
            setCheckboxes(initialValue);
            checkCv29();
            isUpdatingFromEditText = false;
            // The TextWatcher on editText will automatically update the checkboxes
        }

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingFromCheckBox) { // If checkbox changed the text, don't reprocess
                    return;
                }
                isUpdatingFromEditText = true; // Signal that EditText is causing updates

                String currentText = s.toString();
                setCheckboxes(currentText);
                checkCv29();

                isUpdatingFromEditText = false;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        for (int i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingFromEditText) { // If EditText change is causing this, do nothing
                    return;
                }
                isUpdatingFromCheckBox = true; // Signal that CheckBox is causing updates
                updateEditTextFromCheckBoxes();
                isUpdatingFromCheckBox = false;
            });
        }

        buttonConfirm = view.findViewById(R.id.cv_bit_calculator_button_confirm);
        buttonClose = view.findViewById(R.id.cv_bit_calculator_button_close);

        builder.setView(view);
        // We are creating custom buttons, so we don't need the default AlertDialog buttons
        // builder.setPositiveButton(...)
        // builder.setNegativeButton(...)

        Dialog dialog = builder.create();

        buttonConfirm.setOnClickListener(v -> {
            String inputText = editText.getText().toString();
            try {
                if ((inputText.isEmpty()) || (Integer.parseInt(inputText) > 255)) inputText = "";
            } catch (Exception ignored) {
                inputText = "";
            }
            List<Boolean> checkboxStates = new ArrayList<>();
            for (CheckBox checkBox : checkBoxes) {
                checkboxStates.add(checkBox.isChecked());
            }
            if (listener != null) {
                listener.onConfirm(inputText, checkboxStates);
            }
            dismiss(); // Close the dialog
        });

        buttonClose.setOnClickListener(v -> dismiss()); // Close the dialog

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // You can add any cleanup code here if needed when the dialog is dismissed
    }

    private void updateEditTextFromCheckBoxes() {
        int calculatedValue = 0;
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isChecked()) {
                calculatedValue |= (1 << i); // Set the i-th bit
            }
        }
        editText.setText(String.valueOf(calculatedValue));
        checkCv29();
        // editText.setSelection(editText.getText().length()); // Optional: move cursor to end
    }

    private void setCheckboxes(int number) {
        for (int i = 0; i < checkBoxes.length; i++) {
            // Check if the i-th bit is set
            if ((number & (1 << i)) != 0) {
                checkBoxes[i].setChecked(true);
            } else {
                checkBoxes[i].setChecked(false);
            }
        }
    }

    private void setCheckboxes(String currentText) {
        if (currentText.isEmpty()) {
            clearCheckboxes();
            return;
        }

        try {
            int number = Integer.parseInt(currentText);
            // Ensure the number is within the valid range for 8 bits (0-255)
            if (number < 0 || number > 255) {
                clearCheckboxes();
                return;
            }
            setCheckboxes(number);

        } catch (NumberFormatException e) {
            clearCheckboxes();
        }
    }

    private void clearCheckboxes() {
        for (CheckBox checkBox : checkBoxes) {
            checkBox.setChecked(false);
        }
    }

    void checkCv29() {
        if (initialCv == 29) {
            messageText.setVisibility(View.VISIBLE);
            messageTextScrollView.setVisibility(View.VISIBLE);
            String rslt;

            try {
                rslt = "";
                if (!checkBoxes[0].isChecked()) {
                    rslt = rslt + "0: " + getResources().getString(R.string.cv29DirectionForward);
                } else {
                    rslt = rslt + "0: " + getResources().getString(R.string.cv29DirectionReverse);
                }
                rslt = rslt + "\n";

                if (!checkBoxes[1].isChecked()) {
                    rslt = rslt + "1: " + getResources().getString(R.string.cv29SpeedSteps14);
                } else {
                    rslt = rslt + "1: " + getResources().getString(R.string.cv29SpeedSteps28);
                }
                rslt = rslt + "\n";

                if (!checkBoxes[2].isChecked()) {
                    rslt = rslt + "2: " + getResources().getString(R.string.cv29AnalogueConversionOff);
                } else {
                    rslt = rslt + "2: " + getResources().getString(R.string.cv29AnalogueConversionOn);
                }
                rslt = rslt + "\n";

                if (!checkBoxes[3].isChecked()) {
                    rslt = rslt + "3: " + getResources().getString(R.string.cv29SpeedRailcomNo);
                } else {
                    rslt = rslt + "3: " + getResources().getString(R.string.cv29SpeedRailcomYes);
                }
                rslt = rslt + "\n";

                if (!checkBoxes[4].isChecked()) {
                    rslt = rslt + "4: " + getResources().getString(R.string.cv29SpeedTableNo);
                } else {
                    rslt = rslt + "4: " + getResources().getString(R.string.cv29SpeedTableYes);
                }
                rslt = rslt + "\n";

                if (!checkBoxes[5].isChecked()) {
                    rslt = rslt + "5: " + getResources().getString(R.string.cv29AddressSize2bit);
                } else {
                    rslt = rslt + "5: " + getResources().getString(R.string.cv29AddressSize4bit);
                }

                messageText.setText(rslt);

            } catch (Exception e) {
                messageText.setVisibility(View.GONE);
                messageTextScrollView.setVisibility(View.GONE);
                Log.e(threaded_application.applicationName, activityName + ": checkCv29(): Error processing cv29: " + e.getMessage());
            }
        } else {
            messageText.setVisibility(View.GONE);
            messageTextScrollView.setVisibility(View.GONE);
        }
    }
}

