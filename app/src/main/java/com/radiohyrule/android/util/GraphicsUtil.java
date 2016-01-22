package com.radiohyrule.android.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
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

    // https://github.com/felipecsl/Android-ImageManager/blob/master/library/src/com/felipecsl/android/imaging/BitmapProcessor.java
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

    // http://blog.neteril.org/blog/2013/08/12/blurring-images-on-android/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap blurImage(Bitmap input, Context context)
    {
        Bitmap scaledDown = Bitmap.createScaledBitmap(input, 16, 16, true);
        return Bitmap.createScaledBitmap(scaledDown, input.getWidth(), input.getHeight(), true);
    }

    // http://blog.neteril.org/blog/2013/08/12/blurring-images-on-android/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap blurImageGaussian(Bitmap input, Context context, float radius)
    {
        RenderScript rsScript = RenderScript.create(context);
        Allocation allocation = Allocation.createFromBitmap(rsScript, input);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, allocation.getElement());
        blur.setRadius(radius);
        blur.setInput(allocation);

        Bitmap result = Bitmap.createBitmap(input.getWidth(), input.getHeight(), input.getConfig());
        Allocation outputAllocation = Allocation.createFromBitmap(rsScript, result);
        blur.forEach(outputAllocation);
        outputAllocation.copyTo(result);

        rsScript.destroy();
        return result;
    }
}
