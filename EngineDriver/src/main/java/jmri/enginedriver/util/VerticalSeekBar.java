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
    protected boolean prefTickMarksOnSliders = true;
    public boolean tickMarksChecked = false;

    Paint tickPaint;
    Paint textPaint;

    int sliderPurpose = 0;  // 0=Throttle
    String title = "";

    protected int steps;
    protected int height;
    protected int width;
    protected int paddingLeft;
    protected int paddingRight;
    protected float realHeight;
    protected float realTouchY;
//    protected float gridLeft;
    protected float gridBottom;
//            protected float gridTop;
//            protected float gridRight;
    protected float gridMiddle;
    protected float tickSpacing;
    protected float sizeIncrease;
    protected float d;
    protected float l;
    protected float r;
    protected float j;



    // A change listener registering start and stop of tracking. Need an own listener because the listener in SeekBar
    // is private.
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    private static final int SLIDER_PURPOSE_THROTTLE = 0;
    private static final int SLIDER_PURPOSE_OTHER = 1;
    private static final int SLIDER_PURPOSE_SEMI_REALISTIC_THROTTLE = 2;

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
        tickPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
        textPaint.setTextSize(12);
    }

    public VerticalSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mainapp = (threaded_application) context.getApplicationContext();
        prefs = context.getSharedPreferences("jmri.enginedriver_preferences", 0);
        tickMarksChecked = false;

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
        textPaint.setTextSize(32);
    }


    @Override
    protected final void onSizeChanged(final int width, final int height, final int oldWidth, final int oldHeight) {
        super.onSizeChanged(height, width, oldHeight, oldWidth);
    }

    @Override
    protected final synchronized void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    public void setTickType(int requestedTickMarkType) {
        tickMarkType = requestedTickMarkType;
    }

    public void setSliderPurpose(int requestedSliderPurpose) {
        sliderPurpose = requestedSliderPurpose;
    }

    public void setTitle(String requestedTitle) {
        title = requestedTitle;
    }

    @Override
    protected final void onDraw(final Canvas c) {
        c.rotate(ROTATION_ANGLE);
        c.translate(-getHeight(), 0);

        if (!tickMarksChecked) {
            tickMarksChecked = true;
            prefTickMarksOnSliders = prefs.getBoolean("prefTickMarksOnSliders", getResources().getBoolean(R.bool.prefTickMarksOnSlidersDefaultValue));

            if (sliderPurpose == SLIDER_PURPOSE_THROTTLE) {
                prefDisplaySpeedUnits = threaded_application.getIntPrefValue(prefs, "DisplaySpeedUnits", getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
                steps = prefDisplaySpeedUnits;
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
            } else {
                steps = tickMarkType;  // take the steps from the type
            }

        }

        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();
        realHeight = getHeight() - paddingLeft - paddingRight;

        if (prefTickMarksOnSliders) {
            height = getHeight();
            width = getWidth();

            int startSize = 10;
            float endSize = width/2 - 30;
            if (width < 150) {
                startSize = 2;
                endSize = width/2 - 15;
            } else if ( (endSize) > startSize * 9) {
                endSize = startSize * 9;
            }

            gridMiddle = width / 2;

            switch (tickMarkType) {
                default:
                case tick_type.TICK_0_100:
                    gridBottom = height - paddingLeft;
                    tickSpacing = (paddingRight - gridBottom) / (steps - 1);
                    sizeIncrease = endSize / (steps * steps);

                    for (int i = -1; i < steps; i++) {
                        j = (steps - i);
                        d = gridBottom + i * tickSpacing;
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        c.drawLine(d, l, d, r, tickPaint);
                    }
                    break;

                case tick_type.TICK_0_2:
//                case tick_type.TICK_0_3:
                case tick_type.TICK_0_5:
//                case tick_type.TICK_0_6:
//                case tick_type.TICK_0_7:
                case tick_type.TICK_0_8:
//                case tick_type.TICK_0_9:
                case tick_type.TICK_0_28:
                    gridBottom = height - paddingLeft;
                    tickSpacing = (paddingRight - gridBottom) / tickMarkType;
                    sizeIncrease = endSize / ((tickMarkType+1) * (tickMarkType+1));

                    for (int i = -1; i < (tickMarkType+1); i++) {
                        j = ((tickMarkType+1) - i);
                        d = gridBottom + i * tickSpacing;
                        l = gridMiddle - startSize - sizeIncrease * j * j;
                        r = gridMiddle + startSize + sizeIncrease * j * j;
                        c.drawLine(d, l, d, r, tickPaint);
                        c.rotate(90, d-10, r+20);
                        c.drawText(Integer.toString(steps-i),  d-10,r+20, textPaint);
                        c.rotate(-90, d-10, r+20);
                    }

                    c.rotate(90, height - paddingLeft + 10, gridMiddle);
                    c.drawText(title,  height - paddingLeft + 10,gridMiddle, textPaint);
                    c.rotate(-90, height - paddingLeft + 10, gridMiddle);

                    break;

                case tick_type.TICK_0_100_0:
                    int tempSteps = steps/2;
                    gridBottom = height/2 - paddingLeft;
                    tickSpacing = (paddingRight - gridBottom) / (tempSteps - 1);
                    sizeIncrease = endSize / (tempSteps * tempSteps);

                    for (int i = -1; i < tempSteps; i++) {
                        j = (tempSteps - i);
                        d = gridBottom + (height/2) + (i * tickSpacing);
                        l = gridMiddle - startSize - (sizeIncrease) * j * j;
                        r = gridMiddle + startSize + (sizeIncrease) * j * j;
                        c.drawLine(d, l, d, r, tickPaint);
                    }

                    for (int i = -1; i < tempSteps; i++) {
                        j = (tempSteps - i);
                        d = gridBottom + ((tempSteps - i - 1) * tickSpacing);
                        l = gridMiddle - startSize - (sizeIncrease) * j * j;
                        r = gridMiddle + startSize + (sizeIncrease) * j * j;
                        c.drawLine(d, l, d, r, tickPaint);
                    }

                    break;

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
                progress = getMax() - (int) (getMax() * realTouchY / realHeight);
                if (progress<0) {progress = 0;}
                if (progress>getMax()) {progress = getMax();}
                setSeekBarProgress(progress,realTouch);
                mOnSeekBarChangeListener.onStartTrackingTouch(this);
                break;

            case MotionEvent.ACTION_MOVE:
                realTouchY = event.getY()-paddingLeft;
                progress = getMax() - (int) (getMax() * realTouchY / realHeight);
                if (progress<0) {progress = 0;}
                if (progress>getMax()) {progress = getMax();}
                setSeekBarProgress(progress,realTouch);
                break;

            case MotionEvent.ACTION_UP:
                realTouchY = event.getY()-paddingLeft;
                progress = getMax() - (int) (getMax() * realTouchY / realHeight);
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
}