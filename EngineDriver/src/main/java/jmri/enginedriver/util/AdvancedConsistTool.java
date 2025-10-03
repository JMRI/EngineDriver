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

import java.util.ArrayList;
import java.util.List;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.message_type;

public class AdvancedConsistTool extends DialogFragment {

    private threaded_application mainapp;  // hold pointer to mainapp

    private EditText consistAddressEditText;
    private EditText locoAddressEditText;
    private CheckBox facingCheckBox;

    private Button buttonAdd;
    private Button buttonRemove;
    private Button buttonClose;

    String consistAddressText = "";
    String locoAddressText = "";


    // Interface to communicate back to the Activity/Fragment
    public interface OnConfirmListener {
        void onConfirm(String inputText, List<Boolean> checkboxStates);
    }

    private OnConfirmListener listener;

    public static AdvancedConsistTool newInstance() {
        AdvancedConsistTool fragment = new AdvancedConsistTool();
//        Bundle args = new Bundle();
//        args.putInt("initialValue", initialValue); // Use a key to store the value
//        fragment.setArguments(args);
        return fragment;
    }

    // Call this method to set the listener from the hosting Activity/Fragment
    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mainapp = (threaded_application) requireActivity().getApplication();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.advanced_consist_tool_dialog, null); // Use your layout file name

        consistAddressEditText = view.findViewById(R.id.advanced_consist_tool_consist_address_input);
        locoAddressEditText = view.findViewById(R.id.advanced_consist_tool_loco_address_input);
        facingCheckBox = view.findViewById(R.id.advanced_consist_tool_loco_facing_checkbox);
        facingCheckBox.setChecked(true);

        buttonAdd = view.findViewById(R.id.advanced_consist_tool_add_button);
        buttonRemove = view.findViewById(R.id.advanced_consist_tool_remove_button);
        buttonClose = view.findViewById(R.id.advanced_consist_tool_button_close);

        builder.setView(view);
        // We are creating custom buttons, so we don't need the default AlertDialog buttons
        // builder.setPositiveButton(...)
        // builder.setNegativeButton(...)

        Dialog dialog = builder.create();

        buttonAdd.setOnClickListener(v -> {
            checkValues();
            if ( (!consistAddressText.isEmpty()) && (!locoAddressText.isEmpty()) ) {
                mainapp.sendMsg(mainapp.comm_msg_handler,
                        message_type.WRITE_ADVANCED_CONSIST_ADD,
                        "S" + consistAddressText
                                + " MyConsist"
                                + " " + ((Integer.parseInt(locoAddressText) <= 127) ? "S" : "L") + locoAddressText
                                + " " + (facingCheckBox.isChecked() ? "true" : "false"),
                        0);
            }
        });

        buttonRemove.setOnClickListener(v -> {
            checkValues();
            if ( (!consistAddressText.isEmpty()) && (!locoAddressText.isEmpty()) ) {
                mainapp.sendMsg(mainapp.comm_msg_handler,
                        message_type.WRITE_ADVANCED_CONSIST_REMOVE,
                        "S" +consistAddressText
                                + " " + ((Integer.parseInt(locoAddressText) <= 127) ? "S" : "L") + locoAddressText,
                        0);
            }
        });

        buttonClose.setOnClickListener(v -> dismiss()); // Close the dialog

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // You can add any cleanup code here if needed when the dialog is dismissed
    }
     private void checkValues() {
         consistAddressText = consistAddressEditText.getText().toString();
         locoAddressText = locoAddressEditText.getText().toString();
         try {
             if ((consistAddressText.isEmpty()) || (Integer.parseInt(consistAddressText) > 127)) consistAddressText = "";
         } catch (Exception ignored) {
             consistAddressText = "";
         }
         try {
             if ((locoAddressText.isEmpty())
                     || (Integer.parseInt(locoAddressText) < 1)
                     || (Integer.parseInt(locoAddressText) > 9999)) locoAddressText = "";
         } catch (Exception ignored) {
             locoAddressText = "";
         }
     }

}

