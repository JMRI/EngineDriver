package jmri.enginedriver.util;


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

@SuppressLint("AppCompatCustomView")
public class HorizontalSeekBar extends SeekBar {

    private SharedPreferences prefs;
    protected int prefDisplaySpeedUnits = 100;
    protected boolean prefTickMarksOnSliders = true;
    public boolean tickMarksChecked = false;

    Paint tickPaint;

    protected int steps;
    protected int height;
    protected int width, realWidth;
    protected int paddingLeft;
    protected int paddingRight;
    protected int paddingTop;
    protected int paddingBottom;
    protected float gridBottom;
    protected float gridCenter;
    protected float gridMiddle;
    protected float tickSpacing;
    protected float sizeIncrease;
    protected float startSize;
    protected float endSize;
    protected float d;
    protected float l;
    protected float r;
    protected float j;

    int tickMarkType = 0;

    // A change listener registration start and stop of tracking. Need an own listener because the listener in SeekBar
    // is private.
//    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public boolean touchFromUser = false;
//    public boolean realTouch = true;

    public threaded_application mainapp;  // hold pointer to mainapp

    public HorizontalSeekBar(final Context context) {
        super(context);
        mainapp = (threaded_application) context.getApplicationContext();
    }

    public HorizontalSeekBar(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        mainapp = (threaded_application) context.getApplicationContext();
        prefs = context.getSharedPreferences("jmri.enginedriver_preferences", 0);
        tickMarksChecked = false;

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        tickPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
        int tickColor = threaded_application.getRgbColorFromThemeAttribute(context, R.attr.ed_throttle_tick_mark);
        tickPaint.setColor(tickColor);
    }

    public HorizontalSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mainapp = (threaded_application) context.getApplicationContext();
        prefs = context.getSharedPreferences("jmri.enginedriver_preferences", 0);
        tickMarksChecked = false;

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        tickPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
        int tickColor = threaded_application.getRgbColorFromThemeAttribute(context, R.attr.ed_throttle_tick_mark);
        tickPaint.setColor(tickColor);
    }

    public void setTickType(int requestedTickMarkType) {
        tickMarkType = requestedTickMarkType;
    }

    public void resetTickMarks() {
        tickMarksChecked = false;
    }

    @Override
    protected final void onDraw(final Canvas c) {

        if (!tickMarksChecked) {
            tickMarksChecked = true;
            prefTickMarksOnSliders = prefs.getBoolean("prefTickMarksOnSliders", getResources().getBoolean(R.bool.prefTickMarksOnSlidersDefaultValue));
            prefDisplaySpeedUnits = threaded_application.getIntPrefValue(prefs, "prefDisplaySpeedUnits", getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));

            steps = (prefDisplaySpeedUnits!=-1) ? prefDisplaySpeedUnits : 100;
            if (steps >= 100) {
                steps = steps / 3;
            } else {
                if (steps < 28) {
                    steps = steps * 3;
                }
            }
        }
        if (prefTickMarksOnSliders) {
            height = getHeight();
            width = getWidth();

            int additionalPadding = 30;
            startSize = 10;
            endSize = (float) (( (float) height * 0.5) / 2.0);
            if (height < 100 ) {
                startSize = 2;
                additionalPadding = 15;
            }
            if ( endSize > startSize * 9) {
                endSize = startSize * 9;
            }

            paddingLeft = getPaddingLeft();
            paddingRight = getPaddingRight();
            realWidth = getWidth() - paddingLeft - paddingRight - additionalPadding * 2;
            gridMiddle = ((float) height) / 2;

            switch (tickMarkType) {
                case tick_type.TICK_AUTO_0_AUTO:
                case tick_type.TICK_100_0_100:
                case tick_type.TICK_126_0_126: {

                    int tempSteps = steps/2;
                    gridBottom = (float) height /2 - getPaddingLeft();
                    tickSpacing = (float) (width - (getPaddingLeft() * 2)) / (steps - 1);
                    sizeIncrease = (gridMiddle - getPaddingTop() - additionalPadding) / (steps * steps) * 2;

                    // left
                    for (int i = -1; i < tempSteps; i++) {
                        j = (tempSteps - i);
                        d = getPaddingLeft() + (i * tickSpacing);
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        c.drawLine(d, l, d, r, tickPaint);   // x, y, end_x, end_y
                    }

                    // right
                    for (int i = -1; i < tempSteps; i++) {
                        j = (tempSteps - i);
                        d = getPaddingLeft() + (float) width / 2 + ((tempSteps - i - 1) * tickSpacing);
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        c.drawLine(d, l, d, r, tickPaint);   // x, y, end_x, end_y
                    }
                    break;
                }

                case tick_type.TICK_8_0_8:
                case tick_type.TICK_10_0_10:
                case tick_type.TICK_14_0_14:
                case tick_type.TICK_28_0_28: {

                    float adjustedSteps = tickMarkType - 1000 + 1;
                    int tempSteps = tickMarkType - 1000;

                    gridCenter = paddingLeft + additionalPadding + (float) (realWidth / 2);
                    gridBottom = (float) paddingLeft;
                    tickSpacing = (float) (realWidth) / ((adjustedSteps * 2) - 1);
                    sizeIncrease = endSize / (adjustedSteps * adjustedSteps);

                    //left
                    for (int i = -1; i < adjustedSteps; i++) {
                        j = (adjustedSteps - i);
                        d = gridCenter - (adjustedSteps - i - 1) * tickSpacing;
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        // Draw a line from (startX, startY) to (stopX, stopY)
                        c.drawLine(d, l, d, r, tickPaint);
                    }

                    // right
                    for (int i = -1; i < tempSteps; i++) {
                        j = adjustedSteps - i;
                        d = gridCenter + ((adjustedSteps - i - 1) * tickSpacing);
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        // Draw a line from (startX, startY) to (stopX, stopY)
                        c.drawLine(d, l, d, r, tickPaint);
                    }
                    break;
                }

                case tick_type.TICK_AUTO:
                case tick_type.TICK_0_100:
                case tick_type.TICK_0_126:

                    tickSpacing = (float) (width - (getPaddingLeft() * 2)) / (steps - 1);
                    sizeIncrease = (gridMiddle - getPaddingTop() - additionalPadding) / (steps * steps);

                    for (int i = 0; i < steps; i++) {
                        d = getPaddingLeft() + i * tickSpacing;
                        l = gridMiddle - startSize - sizeIncrease * i * i;
                        r = gridMiddle + startSize + sizeIncrease * i * i;
                        c.drawLine(d, l, d, r, tickPaint);   // x, y, end_x, end_y
                    }
                    break;


                case tick_type.TICK_0_8:
                case tick_type.TICK_0_10:
                case tick_type.TICK_0_28:
                default: // 8, 10, 28 etc. steps

                    tickSpacing = (float) (width - (getPaddingLeft() * 2)) / ((tickMarkType+1) - 1);
                    sizeIncrease = (gridMiddle - getPaddingTop() - additionalPadding) / ((tickMarkType+1) * (tickMarkType+1));

                    for (int i = 0; i < (tickMarkType+1); i++) {
                        j = (tickMarkType+1) - i;
                        d = getPaddingLeft() + j * tickSpacing;
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        c.drawLine(d, l, d, r, tickPaint);   // x, y, end_x, end_y
                    }
                    break;
            }
        }

        super.onDraw(c);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        return super.onTouchEvent(event);
    }
}