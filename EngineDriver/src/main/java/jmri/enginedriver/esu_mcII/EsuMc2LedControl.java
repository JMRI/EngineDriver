package jmri.enginedriver.esu_mcII;

import eu.esu.mobilecontrol2.sdk.MobileControl2;

public class EsuMc2LedControl {
    EsuMc2LedState stateRed;
    EsuMc2LedState stateGreen;

    public void setState(EsuMc2Led which, EsuMc2LedState state) {
        this.setState(which, state, false);
    }

    public void setState(EsuMc2Led which, EsuMc2LedState state, boolean storeState) {
        switch (state) {
            case ON:
                MobileControl2.setLedState(which.getValue(), true);
                break;
            case QUICK_FLASH:
                MobileControl2.setLedState(which.getValue(), 125, 125);
                break;
            case STEADY_FLASH:
                MobileControl2.setLedState(which.getValue(), 250, 250);
                break;
            case LONG_FLASH:
                MobileControl2.setLedState(which.getValue(), 375, 125);
                break;
            case SHORT_FLASH:
                MobileControl2.setLedState(which.getValue(), 125, 375);
                break;
            case OFF:
            default:
                // Default off
                MobileControl2.setLedState(which.getValue(), false);
        }
        if (storeState) {
            switch (which) {
                case RED:
                    stateRed = state;
                    break;
                case GREEN:
                    stateGreen = state;
                    break;
            }
        }
    }

    public EsuMc2LedState getState(EsuMc2Led which) {
        if (which == EsuMc2Led.RED) {
            return this.stateRed;
        } else {
            return this.stateGreen;
        }
    }

    public void revertLEDStates() {
        revertLEDState(EsuMc2Led.RED);
        revertLEDState(EsuMc2Led.GREEN);
    }

    private void revertLEDState(EsuMc2Led which) {
        setState(which, getState(which), false);
    }
}
