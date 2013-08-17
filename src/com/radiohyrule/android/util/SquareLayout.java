package com.radiohyrule.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

// http://stackoverflow.com/questions/2948212/android-layout-with-sqare-buttons
public class SquareLayout extends LinearLayout {
    public SquareLayout(Context context) {
        super(context);
    }

    public SquareLayout(Context context, AttributeSet attrs) {
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
