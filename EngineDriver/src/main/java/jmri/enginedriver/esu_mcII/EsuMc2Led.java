package jmri.enginedriver.esu_mcII;

import eu.esu.mobilecontrol2.sdk.MobileControl2;

public enum EsuMc2Led {
    RED(MobileControl2.LED_RED),
    GREEN(MobileControl2.LED_GREEN);

    private final int value;

    EsuMc2Led(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
