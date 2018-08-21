package jmri.enginedriver;
/*
  VerticalSeekBar code based on code from de.eisfeldj.augendiagnose project, as referred to on StackOverflow
  https://github.com/jeisfeld/Augendiagnose/blob/master/AugendiagnoseIdea/augendiagnoseLib/src/main/java/de/jeisfeld/augendiagnoselib/components/VerticalSeekBar.java
*/

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * Implementation of an easy vertical SeekBar, based on the normal SeekBar.
 */
public class VerticalSeekBar extends SeekBar {
    // The angle by which the SeekBar view should be rotated.
    private static final int ROTATION_ANGLE = -90;

    // A change listener registrating start and stop of tracking. Need an own listener because the listener in SeekBar
    // is private.
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public VerticalSeekBar(final Context context) {
        super(context);
    }

    public VerticalSeekBar(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
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

    @Override
    protected final void onDraw(final Canvas c) {
        c.rotate(ROTATION_ANGLE);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    @Override
    public final void setOnSeekBarChangeListener(final OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
        super.setOnSeekBarChangeListener(l);
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        int progress;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                progress = getMax() - (int) (getMax() * event.getY() / getHeight());
                if (progress<0) {progress = 0;}
                if (progress>getMax()) {progress = getMax();}
                setSeekBarProgress(progress,true);
                mOnSeekBarChangeListener.onStartTrackingTouch(this);
                break;

            case MotionEvent.ACTION_MOVE:
                progress = getMax() - (int) (getMax() * event.getY() / getHeight());
                if (progress<0) {progress = 0;}
                if (progress>getMax()) {progress = getMax();}
                setSeekBarProgress(progress,true);
                break;

            case MotionEvent.ACTION_UP:
                progress = getMax() - (int) (getMax() * event.getY() / getHeight());
                if (progress<0) {progress = 0;}
                if (progress>getMax()) {progress = getMax();}
                setSeekBarProgress(progress,true);
                mOnSeekBarChangeListener.onStopTrackingTouch(this);
                break;

            case MotionEvent.ACTION_CANCEL:
                mOnSeekBarChangeListener.onStopTrackingTouch(this);
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    public final void setProgress(final int progress) {
        setSeekBarProgress(progress, false);
    }

    private void setSeekBarProgress(int progress, final boolean fromUser) {

        if (progress != getProgress()) {
            super.setProgress(progress);
            if (mOnSeekBarChangeListener != null) {
                mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
            }
        }

        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }
}