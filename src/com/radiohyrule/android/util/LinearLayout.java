package com.radiohyrule.android.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class LinearLayout extends android.widget.LinearLayout {
    public LinearLayout(Context context) {
        super(context);
    }

    public LinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public LinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int height = b - t;
        int childLeft = getPaddingLeft();
        int remainingHeight = height - getPaddingTop() - getPaddingBottom();
        int remainingWidth = r - l - childLeft;

        final int childCount = getChildCount();
        float weightSum = 0;

        // find out how much horizontal space is left for weighted children
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            final View child = getChildAt(childIndex);
            if (child != null && child.getVisibility() != GONE) {
                final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                remainingWidth -= lp.leftMargin + lp.rightMargin;
                if(lp.weight == 0) {
                    remainingWidth -= child.getMeasuredWidth();
                } else {
                    weightSum += lp.weight;
                }
            }
        }
        if(remainingWidth < 0)
            remainingWidth = 0;
        float widthPerWeight = remainingWidth / weightSum;

        // remeasure and lay out all children
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            final View child = getChildAt(childIndex);
            if (child != null && child.getVisibility() != GONE) {
                final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                if(lp.weight > 0) {
                    child.measure(MeasureSpec.makeMeasureSpec((int)(lp.weight * widthPerWeight), MeasureSpec.EXACTLY),
                                  MeasureSpec.makeMeasureSpec(remainingHeight, MeasureSpec.EXACTLY));
                }
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                final int childTop = getPaddingTop() + ((remainingHeight - childHeight) / 2) + lp.topMargin - lp.bottomMargin;

                childLeft += lp.leftMargin;
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
                childLeft += childWidth + lp.rightMargin;
            }
        }
    }
}
