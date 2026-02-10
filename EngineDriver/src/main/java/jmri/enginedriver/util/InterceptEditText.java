package jmri.enginedriver.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import jmri.enginedriver.threaded_application;

public class InterceptEditText extends androidx.appcompat.widget.AppCompatEditText {

    private threaded_application mainapp;  // hold pointer to mainapp

    public InterceptEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMainApp(threaded_application mainapp) {
        this.mainapp = mainapp;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // Detect if the event is from a physical (non-virtual) keyboard
        if (event.getDeviceId() != KeyCharacterMap.VIRTUAL_KEYBOARD) {
            mainapp.implDispatchKeyEvent(event);
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }
}