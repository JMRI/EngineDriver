package jmri.enginedriver.util;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import jmri.enginedriver.R;
import jmri.enginedriver.SettingsActivity;

public class AutoServerConnectDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private EditText mEditText;

    public static AutoServerConnectDialogFragmentCompat newInstance(String key) {
        final AutoServerConnectDialogFragmentCompat fragment = new AutoServerConnectDialogFragmentCompat();
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

        mEditText = view.findViewById(android.R.id.edit);

        EditTextPreference preference = (EditTextPreference) getPreference();
        if (mEditText != null) {
            mEditText.setText(preference.getText());
        }

        // It's a good practice to hide the default buttons
        // as we are using our own.
        // This requires overriding onCreateDialogView, but for simplicity,
        // we'll handle clicks on our custom buttons and then dismiss the dialog.

        Button currentServerButton = view.findViewById(R.id.auto_server_connect_current_server);
        currentServerButton.setOnClickListener(v -> {
            String currentServer = getConnectedHostname();
            if (mEditText != null) {
                mEditText.setText(currentServer);
            }
        });

        Button clearServerButton = view.findViewById(R.id.auto_server_connect_clear);
        clearServerButton.setOnClickListener(v -> {
            if (mEditText != null) {
                mEditText.setText("");
            }
        });
    }

    /**
     * Gets the currently connected server from the main application thread.
     * @return The address of the currently connected server.
     */
    private String getConnectedHostname() {
        if (getTargetFragment() instanceof SettingsActivity.SettingsFragment) {
            SettingsActivity.SettingsFragment settingsFragment = (SettingsActivity.SettingsFragment) getTargetFragment();
//            if (settingsFragment != null) {
                return settingsFragment.getConnectedHostname();
//            }
        }
        // Return an empty string or a default message if the server can't be found.
        return "";
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
            String value = mEditText.getText().toString();
            EditTextPreference preference = (EditTextPreference) getPreference();

            // Let the preference framework handle saving the value.
            // This also ensures that any OnPreferenceChangeListeners are called.
            if (preference.callChangeListener(value)) {
                preference.setText(value);
            }
        }
    }
}

