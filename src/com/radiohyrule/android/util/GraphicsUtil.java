package com.radiohyrule.android.util;

import android.content.Context;
import android.graphics.*;
import android.util.DisplayMetrics;
import android.util.Log;

public class GraphicsUtil {
    protected static final String LOG_TAG = GraphicsUtil.class.getCanonicalName();

    // http://stackoverflow.com/a/17410076/467840
    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
    // http://stackoverflow.com/a/17410076/467840
    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    public static Bitmap getRoundedCorners(final Bitmap bitmap, final int radius) {
        Bitmap output = null;
        try {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } catch (final OutOfMemoryError e) {
            Log.e(LOG_TAG, "Out of memory in getRoundedCorners()");
            return null;
        }
        final Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
}
