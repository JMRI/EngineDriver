package jmri.enginedriver;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class importServerManualEditTextPreference extends EditTextPreference {

    public importServerManualEditTextPreference(Context context) {
        super(context);
    }

    public importServerManualEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public importServerManualEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            super.setText(super.getText()+" ");
//            PreferenceManager prefManager = getPreferenceManager();
//            prefManager.getSharedPreferences();
//            callChangeListener(super.getText());
        }
        super.onClick(dialog, which);
    }

}