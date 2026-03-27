package jmri.enginedriver.util;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.message_type;

public class AdvancedConsistTool extends DialogFragment {
    static final String activityName = "AdvancedConsistTool";

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
        if (mainapp.activityBundleMessageHandlers[activity_id_type.ADVANCED_CONSIST_TOOL] == null)
            mainapp.activityBundleMessageHandlers[activity_id_type.ADVANCED_CONSIST_TOOL] = new BundleMessageHandler(Looper.getMainLooper());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.alert_dialog_style_reusable);
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

        if(mainapp.isDccexProtocol()) {
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
                if (mainapp.isWiThrottleProtocol()) {

                    Bundle bundle = new Bundle();
                    bundle.putString(alert_bundle_tag_type.CONSIST_TEXT, "S" + consistAddressText);
                    bundle.putString(alert_bundle_tag_type.CONSIST_NAME, "S" + "MyConsist");
                    bundle.putString(alert_bundle_tag_type.LOCO_TEXT, ((Integer.parseInt(locoAddressText) <= 127) ? "S" : "L") + locoAddressText);
                    bundle.putInt(alert_bundle_tag_type.FACING, (facingCheckBox.isChecked() ? 0 : 1));
                    mainapp.alertCommHandlerWithBundle(message_type.WRITE_ADVANCED_CONSIST_ADD, bundle);

                } else {

                    Bundle bundle = new Bundle();
                    bundle.putString(alert_bundle_tag_type.CONSIST_TEXT, consistAddressText);
                    bundle.putString(alert_bundle_tag_type.LOCO_TEXT, locoAddressText);
                    bundle.putInt(alert_bundle_tag_type.FACING, (facingCheckBox.isChecked() ? 0 : 1));
                    mainapp.alertCommHandlerWithBundle(message_type.WRITE_DCCEX_COMMAND_STATION_CONSIST_ADD, bundle);
                }
            }
        });

        buttonRemove.setOnClickListener(v -> {
            checkValues();
            if ( (!consistAddressText.isEmpty()) && (!locoAddressText.isEmpty()) ) {
                if (mainapp.isWiThrottleProtocol()) {

                    Bundle bundle = new Bundle();
                    bundle.putString(alert_bundle_tag_type.CONSIST_TEXT, "S" + consistAddressText);
                    bundle.putString(alert_bundle_tag_type.LOCO_TEXT, ((Integer.parseInt(locoAddressText) <= 127) ? "S" : "L") + locoAddressText);
                    mainapp.alertCommHandlerWithBundle(message_type.WRITE_ADVANCED_CONSIST_REMOVE, bundle);

                } else {

                    Bundle dirBundle = new Bundle();
                    dirBundle.putString(alert_bundle_tag_type.CONSIST_TEXT, consistAddressText);
                    dirBundle.putString(alert_bundle_tag_type.LOCO_TEXT, locoAddressText);
                    mainapp.alertCommHandlerWithBundle(message_type.WRITE_DCCEX_COMMAND_STATION_CONSIST_REMOVE, dirBundle);
                }
            }
        });

        buttonClose.setOnClickListener(v -> dismiss()); // Close the dialog

        mainapp.alertCommHandlerWithBundle(message_type.DCCEX_REQUEST_CONSIST_LIST);

        return dialog;

    } // end onCreateDialog()

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        mainapp.clearActivityBundleMessageHandler(activity_id_type.ADVANCED_CONSIST_TOOL);
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

    private class BundleMessageHandler extends Handler {

        public BundleMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
//            threaded_application.extendedLogging(activityName + ": BundleMessageHandler.handleMessage() what: " + msg.what);

//            Bundle bundle = msg.getData();

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

