package com.intsig.expscanerlib.utils;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Created by Android Studio.
 * ProjectName: ExpScannerSDKCaller
 * Author: haozi
 * Date: 2017/7/28
 * Time: 11:13
 */

public class ImageUtils {

    public static Bitmap makeCropedGrayBitmap(byte[] data, int width, int height, int rot, Rect cropRect) {
        int cropwidth = cropRect.width();
        int cropheight = cropRect.height();
        int[] pixels = new int[cropwidth * cropheight];
        int grey, inputOffset, outputOffset, temp;
        byte[] yuv = data;

        if (rot == 0) {
            inputOffset = cropRect.top * width;
            for (int y = 0; y < cropheight; y++) {
                outputOffset = y * cropwidth;
                for (int x = 0; x < cropwidth; x++) {
                    grey = yuv[inputOffset + x + cropRect.left] & 0xff;
                    pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
                }
                inputOffset += width;
            }
        } else if (rot == 90) {
            int x, y, x1, y1;
            for (y = 0; y < cropheight; y++) {
                y1 = cropRect.top + y;
                for (x = 0; x < cropwidth; x++) {
                    x1 = cropRect.left + x;
                    grey = yuv[y1 * width + x1] & 0xff;
                    pixels[x * cropheight + cropheight - y - 1] = 0xFF000000 | (grey * 0x00010101);
                }

            }
            temp = cropwidth;
            cropwidth = cropheight;
            cropheight = temp;
        }
        Bitmap bitmap = Bitmap.createBitmap(cropwidth, cropheight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, cropwidth, 0, 0, cropwidth, cropheight);

        pixels = null;
        yuv = null;

        return bitmap;
    }

}
