package jmri.enginedriver.util;
/*
  VerticalSeekBar code based on code from de.eisfeldj.augendiagnose project, as referred to on StackOverflow
  https://github.com/jeisfeld/Augendiagnose/blob/master/AugendiagnoseIdea/augendiagnoseLib/src/main/java/de/jeisfeld/augendiagnoselib/components/VerticalSeekBar.java
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

import jmri.enginedriver.type.tick_type;
import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

/**
 * Implementation of an easy vertical SeekBar, based on the normal SeekBar.
 */
@SuppressLint("AppCompatCustomView")
public class VerticalSeekBar extends SeekBar {
    // The angle by which the SeekBar view should be rotated.
    private static final int ROTATION_ANGLE = -90;

    private SharedPreferences prefs;
    protected int prefDisplaySpeedUnits = 100;
    protected int prefDisplaySemiRealisticThrottleNotches = 100;
    protected int prefSemiRealisticThrottleNumberOfBrakeSteps = 7;
    protected double prefSemiRealisticMaximumBrakePcnt = 70;
    protected int prefSemiRealisticThrottleNumberOfLoadSteps = 5;
    int prefSemiRealisticThrottleMaxLoadPcnt = 1000;
    protected boolean prefTickMarksOnSliders = true;
    public boolean tickMarksChecked = false;

    Paint tickPaint;
    Paint textPaint;

    int sliderPurpose = 0;  // 0=Throttle
    boolean showNumericValues = true;
    String title = "";

    protected int steps;
    protected int height;
    protected int width;
    protected int paddingLeft;
    protected int paddingRight;
    protected float realWidth; // minus the padding
    protected float realTouchY;
    protected float gridBottom;
    protected float gridCenter;
    protected float gridMiddle;
    protected float tickSpacing;
    protected float sizeIncrease;
    protected float endSize;
    protected float startSize;
    protected float d;
    protected float l;
    protected float r;
    protected float j;
//    protected float deadZoneUpper;
//    protected float deadZoneLower;



    // A change listener registering start and stop of tracking. Need an own listener because the listener in SeekBar
    // is private.
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    private static final int SLIDER_PURPOSE_THROTTLE = 0;
    private static final int SLIDER_PURPOSE_BRAKE = 1;
    private static final int SLIDER_PURPOSE_LOAD = 2;
    private static final int SLIDER_PURPOSE_SEMI_REALISTIC_THROTTLE = 3;

    int tickMarkType = 0;

    public boolean touchFromUser = false;
    public boolean realTouch = true;

    public threaded_application mainapp;  // hold pointer to mainapp

    public VerticalSeekBar(final Context context) {
        super(context);
        mainapp = (threaded_application) context.getApplicationContext();
    }

    public VerticalSeekBar(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        mainapp = (threaded_application) context.getApplicationContext();
        prefs = context.getSharedPreferences("jmri.enginedriver_preferences", 0);
        tickMarksChecked = false;

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        tickPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
        int tickColor = threaded_application.getRgbColorFromThemeAttribute(context, R.attr.ed_throttle_tick_mark);
        tickPaint.setColor(tickColor);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        textPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
        tickPaint.setColor(tickColor);
        textPaint.setTextSize(10);
    }

    public VerticalSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mainapp = (threaded_application) context.getApplicationContext();
        prefs = context.getSharedPreferences("jmri.enginedriver_preferences", 0);
        tickMarksChecked = false;

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        tickPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
        int tickColor = threaded_application.getRgbColorFromThemeAttribute(context, R.attr.ed_throttle_tick_mark);
        tickPaint.setColor(tickColor);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        textPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
        tickPaint.setColor(tickColor);
        textPaint.setTextSize(32);
    }


    @Override
    protected final void onSizeChanged(final int width, final int height, final int oldWidth, final int oldHeight) {
        super.onSizeChanged(height, width, oldHeight, oldWidth);
    }

    @Override
    protected final synchronized void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
    }

    public void setTickType(int requestedTickMarkType) {
        tickMarkType = requestedTickMarkType;
    }

    public void resetTickMarks() {
        tickMarksChecked = false;
    }

    public void setSliderPurpose(int requestedSliderPurpose) {
        sliderPurpose = requestedSliderPurpose;
    }

    public void setShowNumericValues(boolean requestedShowNumericValues) {
        showNumericValues = requestedShowNumericValues;
    }

//    public void setDeadZones(int requestedDeadZoneUpper, int requestedDeadZoneLower) {
//        deadZoneUpper = (float) requestedDeadZoneUpper;
//        deadZoneLower = (float) requestedDeadZoneLower;
//    }

    public void setTitle(String requestedTitle) {
        title = requestedTitle;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected final void onDraw(final Canvas c) {
        c.rotate(ROTATION_ANGLE);
        c.translate(-getHeight(), 0);

        height = getHeight();
        width = getWidth();

        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();
        realWidth = height - paddingLeft - paddingRight;

        int size = (int) Math.round((double) (width) / 12);
        textPaint.setTextSize(size);

        if (!tickMarksChecked) { // only do this once
            tickMarksChecked = true;
            prefTickMarksOnSliders = prefs.getBoolean("prefTickMarksOnSliders", getResources().getBoolean(R.bool.prefTickMarksOnSlidersDefaultValue));
            prefSemiRealisticThrottleNumberOfBrakeSteps = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleNumberOfBrakeSteps", "7");
            prefSemiRealisticMaximumBrakePcnt = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticMaximumBrakePcnt", "70");
            prefSemiRealisticThrottleNumberOfLoadSteps = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleNumberOfLoadSteps", "5");
            prefSemiRealisticThrottleMaxLoadPcnt = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleMaxLoadPcnt", "1000");

            if (sliderPurpose == SLIDER_PURPOSE_THROTTLE) {
                prefDisplaySpeedUnits = threaded_application.getIntPrefValue(prefs, "prefDisplaySpeedUnits", getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
                steps = (prefDisplaySpeedUnits!=-1) ? prefDisplaySpeedUnits : 100;
                if (steps >= 100) {
                    steps = steps / 3;
                } else {
                    if (steps < 28) {
                        steps = steps * 3;
                    }
                }
            } else if (sliderPurpose == SLIDER_PURPOSE_SEMI_REALISTIC_THROTTLE) {
                prefDisplaySemiRealisticThrottleNotches = threaded_application.getIntPrefValue(prefs, "prefDisplaySemiRealisticThrottleNotches", getResources().getString(R.string.prefSemiRealisticThrottleNotchesDefaultValue));
                steps = prefDisplaySemiRealisticThrottleNotches;
                if (steps >= 100) {
                    steps = steps / 3;
                }
            } else if (sliderPurpose == SLIDER_PURPOSE_BRAKE) {
                steps = tickMarkType;  // take the steps from the type
            } else if (sliderPurpose == SLIDER_PURPOSE_LOAD) {
                steps = tickMarkType;  // take the steps from the type
            } else {
                steps = tickMarkType;  // take the steps from the type
            }

        }

        if (prefTickMarksOnSliders) {

            startSize = 10;
//            float endSize = width/ (float) 2 - 30;
            endSize = (float) (( (float) height * 0.5) / 2.0);
            if (width < 150) {
                startSize = 2;
//                endSize = width/ (float) 2 - 15;
            }
            if ( endSize > startSize * 9) {
                endSize = startSize * 9;
            }

            gridMiddle = width / (float) 2;

            switch (tickMarkType) {
                case tick_type.TICK_AUTO:
                case tick_type.TICK_0_100:
                case tick_type.TICK_0_126: {
//            if ( (tickMarkType == tick_type.TICK_0_100) || (tickMarkType == tick_type.TICK_0_126)|| (tickMarkType == tick_type.TICK_AUTO) ) {
                    gridBottom = height - paddingLeft;
                    tickSpacing = (paddingRight - gridBottom) / (steps - 1);
                    sizeIncrease = endSize / (steps * steps);

                    for (int i = 0; i < steps; i++) {
                        j = (steps - i);
                        d = gridBottom + i * tickSpacing;
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        c.drawLine(d, l, d, r, tickPaint);

                        if ((sliderPurpose == SLIDER_PURPOSE_BRAKE) || (sliderPurpose == SLIDER_PURPOSE_LOAD)) {
                            if ((i == 0) || (i == i / 10 * 10)) {  // only do every tenth
                                float offset = (float) (gridMiddle + startSize + sizeIncrease * j * j * 0.5);
                                c.rotate(90, d - 10, offset);
                                String tickMarkText = getTickMarkText(steps, i, sliderPurpose);
                                c.drawText(tickMarkText, d - 10, offset, textPaint);
                                c.rotate(-90, d - 10, offset);
                            }
                        }
                    }

                    if (!title.isEmpty()) {
                        c.rotate(90, height - paddingLeft + 10, gridMiddle);
                        c.drawText(title, height - paddingLeft + 10, gridMiddle, textPaint);
                        c.rotate(-90, height - paddingLeft + 10, gridMiddle);
                    }
                    break;
                }

                case tick_type.TICK_AUTO_0_AUTO:
                case tick_type.TICK_100_0_100:
                case tick_type.TICK_126_0_126: {
//            } else if (tickMarkType == tick_type.TICK_100_0_100) {
                    int tempSteps = steps / 2;
                    gridBottom = (float) height / 2 - paddingLeft;
                    tickSpacing = (paddingRight - gridBottom) / (tempSteps - 1);
                    sizeIncrease = endSize / (tempSteps * tempSteps);

                    // bottom
                    for (int i = -1; i < tempSteps; i++) {
                        j = (tempSteps - i);
                        d = gridBottom + ((float) height / 2) + (i * tickSpacing);
                        l = gridMiddle - startSize - (sizeIncrease) * j * j;
                        r = gridMiddle + startSize + (sizeIncrease) * j * j;
                        c.drawLine(d, l, d, r, tickPaint);
                    }

                    // top
                    for (int i = -1; i < tempSteps; i++) {
                        j = (tempSteps - i);
                        d = gridBottom + ((tempSteps - i - 1) * tickSpacing);
                        l = gridMiddle - startSize - (sizeIncrease) * j * j;
                        r = gridMiddle + startSize + (sizeIncrease) * j * j;
                        c.drawLine(d, l, d, r, tickPaint);
                    }
                    break;
                }

                /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

                case tick_type.TICK_8_0_8:
                case tick_type.TICK_10_0_10:
                case tick_type.TICK_14_0_14:
                case tick_type.TICK_28_0_28: {

                    float adjustedSteps = tickMarkType-1000+1;
                    int tempSteps = tickMarkType-1000;

                    gridCenter = paddingLeft + (float) (realWidth / 2);
                    gridBottom = (float) paddingLeft;
                    tickSpacing = (float) (realWidth) / (((adjustedSteps) * 2) - 1);
                    sizeIncrease = endSize / (adjustedSteps * adjustedSteps);

                    //bottom / Left
                    for (int i = 0; i < adjustedSteps; i++) {
                        j = adjustedSteps - i;
                        d = gridCenter - (adjustedSteps - i - 1) * tickSpacing;
                        l = gridMiddle - startSize - (sizeIncrease) * j * j;
                        r = gridMiddle + startSize + (sizeIncrease) * j * j;
                        // Draw a line from (startX, startY) to (stopX, stopY)
                        c.drawLine(d, l, d, r, tickPaint);
                    }

                    // top / Right
                    for (int i = 0; i < adjustedSteps; i++) {
                        j = adjustedSteps - i;
                        d = gridCenter + ((adjustedSteps - i - 1) * tickSpacing);
                        l = gridMiddle - startSize - (sizeIncrease) * j * j;
                        r = gridMiddle + startSize + (sizeIncrease) * j * j;
                        // Draw a line from (startX, startY) to (stopX, stopY)
                        c.drawLine(d, l, d, r, tickPaint);
                    }

                    break;
                }

                /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

                case tick_type.TICK_0_8:
                case tick_type.TICK_0_10:
                case tick_type.TICK_0_28:
                default: { // 8, 10, 28 etc. steps
//            } else { // 8, 10, 28 etc. steps
                    if (width < 150) {
                        startSize = 2;
                        endSize = width / (float) 2 - 15;
                    } else if ((endSize) > startSize * 6) {
                        endSize = startSize * 6;
                    }

                    gridBottom = height - paddingLeft;
                    tickSpacing = (paddingRight - gridBottom) / tickMarkType;
                    sizeIncrease = endSize / ((tickMarkType + 1) * (tickMarkType + 1));

                    for (int i = 0; i < (tickMarkType + 1); i++) {
                        j = ((tickMarkType + 1) - i);
                        d = gridBottom + i * tickSpacing;
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        c.drawLine(d, l, d, r, tickPaint);
                        if (showNumericValues) {
                            c.rotate(90, d - 10, r + 10);
                            String tickMarkText = getTickMarkText(steps, i, sliderPurpose);
                            c.drawText(tickMarkText, d - 10, r + 10, textPaint);
                            c.rotate(-90, d - 10, r + 10);
                        }
                    }

                    if (!title.isEmpty()) {
                        c.rotate(90, height - paddingLeft + 10, gridMiddle);
                        c.drawText(title, height - paddingLeft + 10, gridMiddle, textPaint);
                        c.rotate(-90, height - paddingLeft + 10, gridMiddle);
                    }
                }
            }
        }

        super.onDraw(c);
    }

    @Override
    public final void setOnSeekBarChangeListener(final OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
        super.setOnSeekBarChangeListener(l);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        if (!isEnabled()) {
            return false;
        }

        int progress;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                float y = event.getY();
                realTouchY = event.getY()-paddingLeft;
                progress = getMax() - (int) (getMax() * realTouchY / realWidth);
                if (progress<0) {progress = 0;}
                if (progress>getMax()) {progress = getMax();}
                setSeekBarProgress(progress,realTouch);
                mOnSeekBarChangeListener.onStartTrackingTouch(this);
                break;

            case MotionEvent.ACTION_MOVE:
                realTouchY = event.getY()-paddingLeft;
                progress = getMax() - (int) (getMax() * realTouchY / realWidth);
                if (progress<0) {progress = 0;}
                if (progress>getMax()) {progress = getMax();}
                setSeekBarProgress(progress,realTouch);
                break;

            case MotionEvent.ACTION_UP:
                realTouchY = event.getY()-paddingLeft;
                progress = getMax() - (int) (getMax() * realTouchY / realWidth);
                if (progress<0) {progress = 0;}
                if (progress>getMax()) {progress = getMax();}
                setSeekBarProgress(progress,realTouch);
                mOnSeekBarChangeListener.onStopTrackingTouch(this);
                break;

            case MotionEvent.ACTION_CANCEL:
                mOnSeekBarChangeListener.onStopTrackingTouch(this);
                break;

            default:
                break;
        }

        realTouch = true;

        return true;
    }

    @Override
    public final void setProgress(final int progress) {
        setSeekBarProgress(progress, false);
    }

    private void setSeekBarProgress(int progress, final boolean fromUser) {
        touchFromUser = fromUser;

//        int lastSpeed = this.getProgress();

        if (progress != getProgress()) {
            super.setProgress(progress);
            if (mOnSeekBarChangeListener != null) {
                mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
            }
        }

        onSizeChanged(getWidth(), getHeight(), 0, 0);

    }

    @SuppressLint("DefaultLocale")
    String getTickMarkText(int totalSteps, int step, int sliderPurpose) {
        String tickMarkText;
        if (sliderPurpose == SLIDER_PURPOSE_BRAKE) {
//            double effectiveBrake = (double) (totalSteps - step) / (double) prefSemiRealisticThrottleNumberOfBrakeSteps;
//            double intermediateBrake = effectiveBrake * effectiveBrake * prefSemiRealisticMaximumBrakePcnt;
            double intermediateBrake = getBrakeDecimalPcnt(totalSteps - step, steps, prefSemiRealisticMaximumBrakePcnt);
            tickMarkText = String.format("%.1f%%" , intermediateBrake) ;

        } else if (sliderPurpose == SLIDER_PURPOSE_LOAD) {
//            double loadMax = ((double) prefSemiRealisticThrottleMaxLoadPcnt);
//            double thisStep = (double) (totalSteps - step) / prefSemiRealisticThrottleNumberOfLoadSteps;
//            double intermediateLoad = (thisStep * thisStep * (loadMax-100)) + 100;
            double intermediateLoad = getLoadPcnt(step, steps, prefSemiRealisticThrottleMaxLoadPcnt);
            tickMarkText = String.format("%d%%" , (long) intermediateLoad) ;
        } else {
            tickMarkText = Integer.toString(totalSteps - step);
        }

        return tickMarkText;
    }

    // WARNING: a related calculation is also in the throttle_semi_realistic class
    static double getBrakeDecimalPcnt(double step, double steps, double maxBrakePcnt ) {
        double maxBrakeDecimal = maxBrakePcnt/100;
        double max = Math.sqrt(steps) * steps * maxBrakeDecimal;
        return (Math.sqrt(step) * step * maxBrakeDecimal / max * maxBrakeDecimal) * 100;
    }

    // WARNING: a related calculation is also in the throttle_semi_realistic class
    static double getLoadPcnt(double step, double steps, double maxLoadPcnt ) {
        double load = (steps - step) / steps;
        return (load * load * (maxLoadPcnt-100)) + 100;
    }

}