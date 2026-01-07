package jmri.enginedriver.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import jmri.enginedriver.R;

public class dccexAutomation extends DialogFragment {
    static final String activityName = "dccexAutomation";

    private EditText editText;
    private Button buttonConfirm;
    private Button buttonClose;

    private int initialAddress;
    private String routeOrAutomationId;
    private TextView messageText;

    // Interface to communicate back to the Activity/Fragment
    public interface OnConfirmListener {
        void onConfirm(String inputText, String routeOrAutomationId);
    }

    private OnConfirmListener listener;

    public static dccexAutomation newInstance(int initialAddress, String routeOrAutomationId, int themeResId) {
        dccexAutomation fragment = new dccexAutomation();
        Bundle args = new Bundle();
        args.putInt("initialAddress", initialAddress); // Use a key to store the value
        args.putString("routeOrAutomationId", routeOrAutomationId); // Use a key to store the value
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
            routeOrAutomationId = getArguments().getString("routeOrAutomationId");
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
        View view = inflater.inflate(R.layout.dccex_automation_dialog, null); // Use your layout file name

        buttonConfirm = view.findViewById(R.id.dccex_automation_dialog_button_confirm);
        buttonClose = view.findViewById(R.id.dccex_automation_dialog_button_close);


        editText = view.findViewById(R.id.dccex_automation_dialog_edit_text_input);
        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                enableDisableConfirmButton();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                enableDisableConfirmButton();
            }
        });

        // Retrieve the initial value from arguments
        if (getArguments() != null) {
            int initialAddress = getArguments().getInt("initialAddress", 0); // Default to 0 if not found
            editText.setText((initialAddress != 0) ? String.valueOf(initialAddress) : "");
            routeOrAutomationId = getArguments().getString("routeOrAutomationId");
        }

        enableDisableConfirmButton();

        builder.setView(view);

        Dialog dialog = builder.create();

        buttonConfirm.setOnClickListener(v -> {
            String inputText = editText.getText().toString();
            if (!addressIsValid())
                inputText = "";

            if (listener != null) {
                listener.onConfirm(inputText, routeOrAutomationId);
            }
            dismiss(); // Close the dialog
        });

        buttonClose.setOnClickListener(v -> dismiss()); // Close the dialog


        return dialog;
    }

    void enableDisableConfirmButton() {
        if (addressIsValid())
            buttonConfirm.setEnabled(true);
        else
            buttonConfirm.setEnabled(false);
    }

    private boolean addressIsValid() {
        boolean result = false;
        String inputText = editText.getText().toString();
        try {
            if ((!inputText.isEmpty()) && (Integer.parseInt(inputText) > 0) && (Integer.parseInt(inputText) < 10239))
                result = true;
        } catch (Exception ignored) {
        }

        return result;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // You can add any cleanup code here if needed when the dialog is dismissed
    }

}

