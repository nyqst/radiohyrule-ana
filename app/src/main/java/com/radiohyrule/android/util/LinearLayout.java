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
        if(getOrientation() == VERTICAL) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    private void layoutVertical(int l, int t, int r, int b) {
        final int childCount = getChildCount();
        int remainingHeight = b - t - getPaddingTop() - getPaddingBottom();
        final int remainingWidth = r - l - getPaddingLeft() - getPaddingRight();

        // find out how much vertical space is left for weighted children
        float weightSum = 0;
        for(int childIndex = 0; childIndex < childCount; childIndex++) {
            final View child = getChildAt(childIndex);
            if(child != null && child.getVisibility() != GONE) {
                final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                remainingHeight -= lp.topMargin + lp.bottomMargin;
                if(lp.weight == 0) {
                    remainingHeight -= child.getMeasuredHeight();
                } else {
                    weightSum += lp.weight;
                }
            }
        }
        if(remainingHeight < 0)
            remainingHeight = 0;
        float heightPerWeight = remainingHeight / weightSum;

        // remeasure and lay out all children
        int childTop = getPaddingTop();
        for(int childIndex = 0; childIndex < childCount; childIndex++) {
            final View child = getChildAt(childIndex);
            if(child != null && child.getVisibility() != GONE) {
                final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                if(lp.weight > 0) {
                    child.measure(MeasureSpec.makeMeasureSpec(remainingWidth, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec((int) (lp.weight * heightPerWeight), MeasureSpec.EXACTLY));
                }
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                final int childLeft = getPaddingLeft() + ((remainingWidth - childWidth) / 2) + lp.leftMargin - lp.rightMargin;

                childTop += lp.topMargin;
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
                childTop += childHeight + lp.bottomMargin;
            }
        }
    }

    private void layoutHorizontal(int l, int t, int r, int b) {
        final int childCount = getChildCount();
        final int remainingHeight = b - t - getPaddingTop() - getPaddingBottom();
        int remainingWidth = r - l - getPaddingLeft() - getPaddingRight();

        // find out how much horizontal space is left for weighted children
        float weightSum = 0;
        for(int childIndex = 0; childIndex < childCount; childIndex++) {
            final View child = getChildAt(childIndex);
            if(child != null && child.getVisibility() != GONE) {
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
        int childLeft = getPaddingLeft();
        for(int childIndex = 0; childIndex < childCount; childIndex++) {
            final View child = getChildAt(childIndex);
            if(child != null && child.getVisibility() != GONE) {
                final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                if(lp.weight > 0) {
                    child.measure(MeasureSpec.makeMeasureSpec((int) (lp.weight * widthPerWeight), MeasureSpec.EXACTLY),
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
