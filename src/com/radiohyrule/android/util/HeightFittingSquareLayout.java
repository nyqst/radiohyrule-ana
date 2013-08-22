package com.radiohyrule.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class HeightFittingSquareLayout extends LinearLayout {
    public HeightFittingSquareLayout(Context context) {
        super(context);
    }

    public HeightFittingSquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    }
}
