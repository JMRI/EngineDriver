package jmri.enginedriver.import_export;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import jmri.enginedriver.R;

public class importServerManualEditTextPreference extends EditTextPreference {
    private final Context context;

    private static final String PREF_IMPORT_ALL_FULL = "Yes";
    private static final String PREF_IMPORT_ALL_PARTIAL = "No";
    private static final String PREF_IMPORT_ALL_RESET = "-";


    public importServerManualEditTextPreference(Context context) {
        super(context);
        this.context=context;
    }

    public importServerManualEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
    }

    public importServerManualEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context=context;
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
    {
        super.onPrepareDialogBuilder(builder);
        builder.setNeutralButton(R.string.importServerAutoDialogNeutralButton, this);
        builder.setPositiveButton(R.string.importServerAutoDialogPositiveButton, this);
    }


    @SuppressLint("ApplySharedPref")
    @Override
    public void onClick(DialogInterface dialog, int which) {
        SharedPreferences prefs = context.getSharedPreferences("jmri.enginedriver_preferences", 0);

        switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    prefs.edit().putString("prefPreferencesImportAll", PREF_IMPORT_ALL_FULL).commit();
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    prefs.edit().putString("prefPreferencesImportAll", PREF_IMPORT_ALL_PARTIAL).commit();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    prefs.edit().putString("prefPreferencesImportAll", PREF_IMPORT_ALL_RESET).commit();
                    break;

//            PreferenceManager prefManager = getPreferenceManager();
//            prefManager.getSharedPreferences();
//            callChangeListener(super.getText());
        }
        super.onClick(dialog, which);
    }

}