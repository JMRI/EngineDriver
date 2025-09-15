package jmri.enginedriver.util;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import jmri.enginedriver.R;

public class cvBitCalculator extends DialogFragment {

    private EditText editText;
    private CheckBox[] checkBoxes = new CheckBox[8];
//    private Button buttonConfirm;
    private Button buttonClose;

    // To prevent feedback loops between EditText and CheckBoxes
    private boolean isUpdatingFromEditText = false;
    private boolean isUpdatingFromCheckBox = false;

    // Interface to communicate back to the Activity/Fragment
    public interface OnConfirmListener {
        void onConfirm(String inputText, List<Boolean> checkboxStates);
    }

    private OnConfirmListener listener;

    public static cvBitCalculator newInstance() {
        return new cvBitCalculator();
    }

    // Call this method to set the listener from the hosting Activity/Fragment
    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.cv_bit_calculator_dialog, null); // Use your layout file name

        editText = view.findViewById(R.id.cv_bit_calculator_edit_text_input);
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
                if (currentText.isEmpty()) {
                    // Clear all checkboxes if the input is empty
                    for (CheckBox checkBox : checkBoxes) {
                        checkBox.setChecked(false);
                    }
                    isUpdatingFromEditText = false;
                    return;
                }

                try {
                    int number = Integer.parseInt(currentText);
                    // Ensure the number is within the valid range for 8 bits (0-255)
                    if (number < 0 || number > 255) {
                        // Optionally, show an error or clear checkboxes
                        // For now, let's clear them if out of range
                        for (CheckBox checkBox : checkBoxes) {
                            checkBox.setChecked(false);
                        }
                        // You might want to provide feedback to the user here
                        // e.g., editText.setError("Value must be between 0 and 255");
                        isUpdatingFromEditText = false;
                        return;
                    }

                    for (int i = 0; i < checkBoxes.length; i++) {
                        // Check if the i-th bit is set
                        if ((number & (1 << i)) != 0) {
                            checkBoxes[i].setChecked(true);
                        } else {
                            checkBoxes[i].setChecked(false);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Handle cases where the input is not a valid integer
                    // For example, if the user types "abc"
                    // Clear all checkboxes
                    for (CheckBox checkBox : checkBoxes) {
                        checkBox.setChecked(false);
                    }
                    // You might want to provide feedback to the user here
                    // e.g., editText.setError("Invalid number");
                }
                isUpdatingFromEditText = false;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        checkBoxes[0] = view.findViewById(R.id.cv_bit_calculator_bit_0);
        checkBoxes[1] = view.findViewById(R.id.cv_bit_calculator_bit_1);
        checkBoxes[2] = view.findViewById(R.id.cv_bit_calculator_bit_2);
        checkBoxes[3] = view.findViewById(R.id.cv_bit_calculator_bit_3);
        checkBoxes[4] = view.findViewById(R.id.cv_bit_calculator_bit_4);
        checkBoxes[5] = view.findViewById(R.id.cv_bit_calculator_bit_5);
        checkBoxes[6] = view.findViewById(R.id.cv_bit_calculator_bit_6);
        checkBoxes[7] = view.findViewById(R.id.cv_bit_calculator_bit_7);

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

//        buttonConfirm = view.findViewById(R.id.cv_bit_calculator_button_confirm);
        buttonClose = view.findViewById(R.id.cv_bit_calculator_button_close);

        builder.setView(view);
        // We are creating custom buttons, so we don't need the default AlertDialog buttons
        // builder.setPositiveButton(...)
        // builder.setNegativeButton(...)

        Dialog dialog = builder.create();

//        buttonConfirm.setOnClickListener(v -> {
//            String inputText = editText.getText().toString();
//            List<Boolean> checkboxStates = new ArrayList<>();
//            for (CheckBox checkBox : checkBoxes) {
//                checkboxStates.add(checkBox.isChecked());
//            }
//            if (listener != null) {
//                listener.onConfirm(inputText, checkboxStates);
//            }
//            dismiss(); // Close the dialog
//        });

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
        // editText.setSelection(editText.getText().length()); // Optional: move cursor to end
    }

}

