package com.radiohyrule.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class FittingSquareLayout extends LinearLayout {
    public FittingSquareLayout(Context context) {
        super(context);
    }

    public FittingSquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width > (int)(height + 0.5)) {
            width = (int)(height + 0.5);
        } else {
            height = (int)(width + 0.5);
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }
}
