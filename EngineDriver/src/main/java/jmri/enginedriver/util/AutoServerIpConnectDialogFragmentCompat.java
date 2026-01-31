package jmri.enginedriver.util;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import jmri.enginedriver.R;
import jmri.enginedriver.SettingsActivity;

public class AutoServerIpConnectDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private EditText mEditIp;
    private EditText mEditPort;
    private CheckBox mIsDccEx;

    public static AutoServerIpConnectDialogFragmentCompat newInstance(String key) {
        final AutoServerIpConnectDialogFragmentCompat fragment = new AutoServerIpConnectDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, getSelectedTheme());
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mEditIp = view.findViewById(android.R.id.edit);
        mEditPort = view.findViewById(R.id.edit_port);
        mIsDccEx = view.findViewById(R.id.isDccEx);


        EditTextPreference preference = (EditTextPreference) getPreference();

        String ip = "";
        String port = "";
        boolean isDccEx = false;
        if (preference.getText() != null && !preference.getText().isEmpty()) {
            String[] parts = preference.getText().split(":");
            if (parts.length >= 2) {
                ip = parts[0];
                port = parts[1];
                if ( (parts.length == 3) && (!parts[2].isEmpty()) ) { // don't care what is there
                    isDccEx = true;
                }
            }
        }

        if (mEditIp != null) {
            mEditIp.setText(ip);
            mEditPort.setText(port);
            mIsDccEx.setChecked(isDccEx);
        }

        // It's a good practice to hide the default buttons
        // as we are using our own.
        // This requires overriding onCreateDialogView, but for simplicity,
        // we'll handle clicks on our custom buttons and then dismiss the dialog.

        Button currentServerButton = view.findViewById(R.id.auto_server_connect_current_server);
        currentServerButton.setOnClickListener(v -> {
            String currentServer = getConnectedHostAddress();
            String currentPort = getConnectedHostPort();
            boolean currentIsDccEx = getConnectedHostIsDccEx();
            if (mEditIp != null) {
                mEditIp.setText(currentServer);
                mEditPort.setText(currentPort);
                mIsDccEx.setChecked( currentIsDccEx );
            }
        });

        Button clearServerButton = view.findViewById(R.id.auto_server_connect_clear);
        clearServerButton.setOnClickListener(v -> {
            if (mEditIp != null) {
                mEditIp.setText("");
            }
            if (mEditPort != null) {
                mEditPort.setText("");
            }
            if (mIsDccEx != null) {
                mIsDccEx.setChecked(false);
            }
        });
    }

    /**
     * Gets the currently connected server from the main application thread.
     * @return The address of the currently connected server.
     */
    private String getConnectedHostAddress() {
        if (getTargetFragment() instanceof SettingsActivity.SettingsFragment) {
            SettingsActivity.SettingsFragment settingsFragment = (SettingsActivity.SettingsFragment) getTargetFragment();
                return settingsFragment.getConnectedHostAddress();
        }
        return "";
    }
    private String getConnectedHostPort() {
        if (getTargetFragment() instanceof SettingsActivity.SettingsFragment) {
            SettingsActivity.SettingsFragment settingsFragment = (SettingsActivity.SettingsFragment) getTargetFragment();
            return settingsFragment.getConnectedHostPort();
        }
        return "";
    }
    private boolean getConnectedHostIsDccEx() {
        if (getTargetFragment() instanceof SettingsActivity.SettingsFragment) {
            SettingsActivity.SettingsFragment settingsFragment = (SettingsActivity.SettingsFragment) getTargetFragment();
            return settingsFragment.getConnectedHostIsDccEx();
        }
        return false;
    }

    private int getSelectedTheme() {
        if (getTargetFragment() instanceof SettingsActivity.SettingsFragment) {
            SettingsActivity.SettingsFragment settingsFragment = (SettingsActivity.SettingsFragment) getTargetFragment();
            return settingsFragment.mainapp.getSelectedTheme(true);
        }
        return 0;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = mEditIp.getText().toString() + ":" + mEditPort.getText().toString() + ":" + (mIsDccEx.isChecked() ? "1" : "");
            EditTextPreference preference = (EditTextPreference) getPreference();

            // Let the preference framework handle saving the value.
            // This also ensures that any OnPreferenceChangeListeners are called.
            if (preference.callChangeListener(value)) {
                preference.setText(value);
            }
        }
    }
}

