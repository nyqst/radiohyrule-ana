package com.radiohyrule.android.opengl;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

/**
 * Created by lysann on 1/3/14.
 */
public class Util {
    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static Bitmap loadBitmapForTexturing(Resources resources, int resourceId, int reqWidth, int reqHeight, boolean flipped) {
        // load resource
        final Bitmap bitmap = decodeSampledBitmapFromResource(resources, resourceId, reqWidth, reqHeight);
        // flip vertically
        if (flipped) {
            final Bitmap flippedBitmap = flipBitmap(bitmap);
            bitmap.recycle();
            return flippedBitmap;
        } else {
            return bitmap;
        }
    }
    public static Bitmap flipBitmap(Bitmap bitmap) {
        final android.graphics.Matrix flipMatrix = new android.graphics.Matrix(); flipMatrix.preScale(1, -1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), flipMatrix, false);
    }
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (reqWidth > 0 && reqHeight > 0 && (height > reqHeight || width > reqWidth)) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static int getOpenGlVersion(Activity activity) {
        final ActivityManager activityManager = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion;
    }
    public final static int OPENGL2 = 0x20000;
}
