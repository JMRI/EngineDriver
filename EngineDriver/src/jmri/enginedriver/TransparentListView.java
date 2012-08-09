package jmri.enginedriver;

/*  override the overscrollfooter to insure it is transparent.  Needed for listviews which do not use all 
 *    available space.  Popular suggestion of warp_content causes multiple "loads" of the listview items,
 *    which is not good for items which include web images (was retrieving multiple times)
 *    Found this solution here: http://stackoverflow.com/a/7974508
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ListView;

public class TransparentListView extends ListView {

    private void makeTransparent() {
        if (Build.VERSION.SDK_INT >= 9) {
            try {

                Method overscrollFooterMethod = 
                    TransparentListView.class.getMethod("setOverscrollFooter", new Class[] {Drawable.class});
                Method overscrollHeaderMethod = 
                    TransparentListView.class.getMethod("setOverscrollHeader", new Class[] {Drawable.class});


                try {
                    overscrollFooterMethod.invoke(this, new Object[] {null});
                    overscrollHeaderMethod.invoke(this, new Object[] {null});
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    public TransparentListView(Context context) {
        super(context);
        this.makeTransparent();
    }

    public TransparentListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.makeTransparent();
    }

    public TransparentListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.makeTransparent();
    }
}