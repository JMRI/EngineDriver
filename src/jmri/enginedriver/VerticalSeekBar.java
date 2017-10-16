package jmri.enginedriver;
/*
  VerticalSeekBar code copied from de.eisfeldj.augendiagnose project, as referred to on StackOverflow
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
    /**
     * The angle by which the SeekBar view should be rotated.
     */
    private static final int ROTATION_ANGLE = -90;

    /**
     * A change listener registrating start and stop of tracking. Need an own listener because the listener in SeekBar
     * is private.
     */
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    // JAVADOC:OFF
    /**
     * Standard constructor to be implemented for all views.
     *
     */
    public VerticalSeekBar(final Context context) {
        super(context);
    }

    /**
     * Standard constructor to be implemented for all views.
     *
     */
    public VerticalSeekBar(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Standard constructor to be implemented for all views.
     *
     */
    public VerticalSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    // JAVADOC:ON

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    @Override
    protected final void onSizeChanged(final int width, final int height, final int oldWidth, final int oldHeight) {
        super.onSizeChanged(height, width, oldHeight, oldWidth);
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    @Override
    protected final synchronized void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    @Override
    protected final void onDraw(final Canvas c) {
        c.rotate(ROTATION_ANGLE);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    @Override
    public final void setOnSeekBarChangeListener(final OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
        super.setOnSeekBarChangeListener(l);
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                mOnSeekBarChangeListener.onStartTrackingTouch(this);
                break;

            case MotionEvent.ACTION_MOVE:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                break;

            case MotionEvent.ACTION_UP:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
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

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    @Override
    public final void setProgress(final int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }
}