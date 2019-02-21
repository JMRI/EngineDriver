//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package jmri.enginedriver;

public abstract class CountDownTimer {
    public CountDownTimer(long millisInFuture, long countDownInterval) {
        throw new RuntimeException("Stub!");
    }

    public final void cancel() {
        throw new RuntimeException("Stub!");
    }

    public final synchronized CountDownTimer start() {
        throw new RuntimeException("Stub!");
    }

    public abstract void onTick(long var1);

    public abstract void onFinish();
}

