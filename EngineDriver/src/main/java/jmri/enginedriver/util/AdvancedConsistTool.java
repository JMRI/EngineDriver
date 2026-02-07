package jmri.enginedriver.util;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.InputFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.message_type;

public class AdvancedConsistTool extends DialogFragment {

    private threaded_application mainapp;  // hold pointer to mainapp

    private TextView consistAddressLabel;
    private EditText consistAddressEditText;
    private EditText locoAddressEditText;
    private CheckBox facingCheckBox;

    private ScrollView advancedConsistScrollView;
    private TextView advancedConsistListTextView;

    private Button buttonAdd;
    private Button buttonRemove;
    private Button buttonClose;

    String consistAddressText = "";
    String locoAddressText = "";

    int maxAddressForConsist = 127;

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

        //put pointer to this activity's handler in main app's shared variable (If needed)
        mainapp.advancedConsistToolMesssgeHandler = new AdvancedConsistToolMessageHandler(Looper.getMainLooper());

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

        TextView title = view.findViewById(R.id.advanced_consist_tool_dialog_title);

        advancedConsistScrollView = view.findViewById(R.id.advanced_consist_tool_scroll_view);
        advancedConsistListTextView = view.findViewById(R.id.advanced_consist_tool_consist_list);

        if(mainapp.isDCCEX) {
            title.setText(R.string.advancedConsistToolDccexTitle);
            consistAddressLabel = view.findViewById(R.id.advanced_consist_tool_consist_address_label);
            consistAddressLabel.setText(R.string.advancedConsistToolLeadLocoLabel);

            consistAddressEditText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(5)});
            maxAddressForConsist = 10239;
            advancedConsistScrollView.setVisibility(VISIBLE);
        } else {
            advancedConsistScrollView.setVisibility(GONE);
        }

        builder.setView(view);
        // We are creating custom buttons, so we don't need the default AlertDialog buttons
        // builder.setPositiveButton(...)
        // builder.setNegativeButton(...)

        Dialog dialog = builder.create();

        buttonAdd.setOnClickListener(v -> {
            checkValues();
            if ( (!consistAddressText.isEmpty()) && (!locoAddressText.isEmpty()) ) {
                if (!mainapp.isDCCEX) {
                    mainapp.sendMsg(mainapp.comm_msg_handler,
                            message_type.WRITE_ADVANCED_CONSIST_ADD,
                            "S" + consistAddressText
                                    + " MyConsist"
                                    + " " + ((Integer.parseInt(locoAddressText) <= 127) ? "S" : "L") + locoAddressText
                                    + " " + (facingCheckBox.isChecked() ? "true" : "false"),
                            0);
                } else {
                    mainapp.sendMsg(mainapp.comm_msg_handler,
                             message_type.WRITE_DCCEX_COMMAND_STATION_CONSIST_ADD,
                            consistAddressText
                                    + " " + locoAddressText
                                    + " " + (facingCheckBox.isChecked() ? "0" : "1"),
                            0);

                }
            }
        });

        buttonRemove.setOnClickListener(v -> {
            checkValues();
            if ( (!consistAddressText.isEmpty()) && (!locoAddressText.isEmpty()) ) {
                if (!mainapp.isDCCEX) {
                    mainapp.sendMsg(mainapp.comm_msg_handler,
                            message_type.WRITE_ADVANCED_CONSIST_REMOVE,
                            "S" + consistAddressText
                                    + " " + ((Integer.parseInt(locoAddressText) <= 127) ? "S" : "L") + locoAddressText,
                            0);
                } else {
                    mainapp.sendMsg(mainapp.comm_msg_handler,
                            message_type.WRITE_DCCEX_COMMAND_STATION_CONSIST_REMOVE,
                            consistAddressText
                                    + " " + locoAddressText);
                }
            }
        });

        buttonClose.setOnClickListener(v -> dismiss()); // Close the dialog

        mainapp.sendMsg(mainapp.comm_msg_handler,
                message_type.DCCEX_REQUEST_CONSIST_LIST);

        return dialog;

    } // end onCreateDialog()

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // You can add any cleanup code here if needed when the dialog is dismissed
        mainapp.advancedConsistToolMesssgeHandler = null;
    }

    private void checkValues() {
        consistAddressText = consistAddressEditText.getText().toString();
        locoAddressText = locoAddressEditText.getText().toString();
        try {
            if ((consistAddressText.isEmpty()) || (Integer.parseInt(consistAddressText) > maxAddressForConsist)) consistAddressText = "";
        } catch (Exception ignored) {
            consistAddressText = "";
        }
        try {
            if ((locoAddressText.isEmpty())
                     || (Integer.parseInt(locoAddressText) < 1)
                     || (Integer.parseInt(locoAddressText) > 10239)) locoAddressText = "";
        } catch (Exception ignored) {
            locoAddressText = "";
        }
    }


    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class AdvancedConsistToolMessageHandler extends Handler {

        public AdvancedConsistToolMessageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == message_type.DCCEX_RECEIVED_CONSIST_ENTRY) {
                StringBuilder consistListHTML = new StringBuilder();
                consistListHTML.append("<p>");

                if ((mainapp.dccexInCommandStationConsists == null) || (mainapp.dccexInCommandStationConsists.isEmpty()))
                    return;
                // copy the list to avoid concurrent access
                ArrayList<ArrayList<HashMap<String, String>>> dccexInCommandStationConsists;
                dccexInCommandStationConsists = new ArrayList<>(mainapp.dccexInCommandStationConsists);

                for (ArrayList<HashMap<String, String>> arrayList : dccexInCommandStationConsists) {
                    boolean first = true;
                    for (HashMap<String, String> map : arrayList) {
                        if ((map.get("loco_id") != null) && (map.get("loco_facing") != null)) {
                            if (map.get("loco_facing").equals("1"))
                                consistListHTML.append("-");
                            if (first) consistListHTML.append("<b>");
                            consistListHTML.append(map.get("loco_id"));
                            if (first) {
                                consistListHTML.append("</b> » ");
                                first = false;
                            }
                            consistListHTML.append(" ");
                        }
                    }
                    consistListHTML.append("<br />");
                }
                consistListHTML.append("</p>");
                advancedConsistListTextView.setText(Html.fromHtml(consistListHTML.toString()));
            }
        }
    }
}

