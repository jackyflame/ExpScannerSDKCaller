package com.intsig.expscanerlib.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.intsig.expscanerlib.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Android Studio.
 * ProjectName: ExpScannerSDKCaller
 * Author: haozi
 * Date: 2017/7/26
 * Time: 17:12
 */

public class DetectView extends View {

    private static final String TAG = "DetectView";

    private Paint paint = null;
    private int[] border = null;
    private boolean match = false;
    private int previewWidth;
    private int previewHeight;
    private Context context;

    // 蒙层位置路径
    Path mClipPath = new Path();
    RectF mClipRect = new RectF();
    Rect mClipRectSet = null;

    private int mColorNormal = 0xff00ff00;
    private int mColorMatch = 0xffffffff;

    boolean isVertical = true;
    int[] borderLeftAndRight = new int[4];// 预览框的左右坐标---竖屏的时候

    float mRadius = 12;
    int mRectWidth = 9;
    float cornerSize = 30;// 4个角的大小
    float cornerStrokeWidth = 8;

    int drawCount = 0;

    public void showBorder(int[] border, boolean match) {
        this.border = border;
        this.match = match;
        postInvalidate();
    }

    public DetectView(Context context) {
        super(context);
        initView(context);
    }

    public DetectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public DetectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void setPreviewSize(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
    }

    private void initView(Context context){
        paint = new Paint();
        this.context = context;
        paint.setColor(0xffff0000);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    // 计算蒙层位置
    public void upateClipRegion(float scale, float scaleH) {

        float left, top, right, bottom;

        float density = getResources().getDisplayMetrics().density;

        mRadius = 0;

        mRectWidth = (int) (9 * density);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        cornerStrokeWidth = 8 * density;
        // float scale = getWidth() / (float) previewHeight;

        // Log.e("aaa","scale"+scale+"left"+ left+"right"+right+"top"+top+"bottom"+bottom);
        mClipPath.reset();
        Map<String, Float> map = getPositionWithArea(getWidth(),getHeight(), scale, scaleH);
        left = map.get("left");
        right = map.get("right");
        top = map.get("top");
        bottom = map.get("bottom");
        mClipRect.set(left, top, right, bottom);
        mClipPath.addRoundRect(mClipRect, mRadius, mRadius, Path.Direction.CW);
    }

    @Override
    public void onDraw(Canvas c) {

        // Log.e("aaa", "previewHeight:" + previewHeight + "previewWidth:" + previewWidth + "getWidth：" + getWidth() + "getHeight:" + getHeight());
        // previewHeight:480previewWidth:720getWidth：1200getHeight:1830

        //float scale = getWidth() / (float) previewHeight;
        //float scaleH = getHeight() / (float) previewWidth;
        float scale = 1;
        float scaleH = 1;

        upateClipRegion(scale, scaleH);

        c.save();

        // 绘制 灰色蒙层
        c.clipPath(mClipPath, Region.Op.DIFFERENCE);
        c.drawColor(0xAA666666);
        c.drawRoundRect(mClipRect, mRadius, mRadius, paint);

        c.restore();

        if (match) {// 设置颜色
            paint.setColor(mColorMatch);
        } else {
            paint.setColor(mColorNormal);
        }
        float len = cornerSize;
        float strokeWidth = cornerStrokeWidth;
        paint.setStrokeWidth(strokeWidth);
        // 左上
        c.drawLine(mClipRect.left - strokeWidth / 2, mClipRect.top, mClipRect.left + len, mClipRect.top, paint);
        c.drawLine(mClipRect.left, mClipRect.top, mClipRect.left, mClipRect.top + len, paint);
        // 右上
        c.drawLine(mClipRect.right - len, mClipRect.top, mClipRect.right  + strokeWidth / 2, mClipRect.top, paint);
        c.drawLine(mClipRect.right, mClipRect.top, mClipRect.right, mClipRect.top + len, paint);
        // 右下
        c.drawLine(mClipRect.right - len, mClipRect.bottom, mClipRect.right + strokeWidth / 2, mClipRect.bottom, paint);
        c.drawLine(mClipRect.right, mClipRect.bottom - len, mClipRect.right, mClipRect.bottom, paint);
        // 左下
        c.drawLine(mClipRect.left - strokeWidth / 2, mClipRect.bottom, mClipRect.left + len, mClipRect.bottom, paint);
        c.drawLine(mClipRect.left, mClipRect.bottom - len, mClipRect.left, mClipRect.bottom, paint);

        drawCount++;

        if (border != null) {
            paint.setStrokeWidth(3);
            c.drawLine(border[0] * scale, border[1] * scale, border[2] * scale, border[3] * scale, paint);
            c.drawLine(border[2] * scale, border[3] * scale, border[4] * scale, border[5] * scale, paint);
            c.drawLine(border[4] * scale, border[5] * scale, border[6] * scale, border[7] * scale, paint);
            c.drawLine(border[6] * scale, border[7] * scale, border[0] * scale, border[1] * scale, paint);

        }

        float left, top, right, bottom;

        Map<String, Float> map = getPositionWithArea(getWidth(), getHeight(), scale, scaleH);
        left = map.get("left");
        right = map.get("right");
        top = map.get("top");
        bottom = map.get("bottom");

        // 画动态的中心线
        paint.setColor(context.getResources().getColor(R.color.back_line_3));
        paint.setStrokeWidth(1);
        c.drawLine(left, top + (bottom - top) / 2, right, top + (bottom - top) / 2, paint);

    }

    /**
     * @param newWidth
     * @param newHeight
     * @return
     */
    public Map<String, Float> getPositionWithArea(int newWidth, int newHeight,float scale, float scaleH) {

        float left = 0, top = 0, right = 0, bottom = 0;
        // 注意：机打号的预览框高度设置建议是 屏幕高度的1/10,宽度 尽量与屏幕同宽
        float borderHeight = newHeight / 10;

        Map<String, Float> map = new HashMap<String, Float>();
        if (isVertical) {// vertical
            if(mClipRectSet == null || mClipRectSet.height() <= 0 || mClipRectSet.width() <= 0) {
                int padding_leftright = 50;
                int padding_top = 150;
                left = 0 + padding_leftright;
                right = newWidth-left;
                // 注释的部分 打开就是将预览框放置中心位置
                top = 0 + padding_top;
                bottom = borderHeight + padding_top;
                borderLeftAndRight[0] = (int) top;
                borderLeftAndRight[1] = (int) left;
                borderLeftAndRight[2] = (int) bottom;
                borderLeftAndRight[3] = (int) right;
            }else{
                top = mClipRectSet.top;
                left = mClipRectSet.left;
                bottom = mClipRectSet.bottom;
                right = mClipRectSet.right;
                borderLeftAndRight[0] = (int) top;
                borderLeftAndRight[1] = (int) left;
                borderLeftAndRight[2] = (int) bottom;
                borderLeftAndRight[3] = (int) right;
            }
        }
        map.put("left", left);
        map.put("right", right);
        map.put("top", top);
        map.put("bottom", bottom);

        //Log.i(TAG, "getPositionWithArea-->> scale:" + scale + ",scaleH:" + scaleH + "  borders:" + map.toString());

        return map;

    }

    public void setClipRect(Rect clipRect) {
        this.mClipRectSet = clipRect;
        //invalidate();
    }

    public int[] getDetctArea() {
        return borderLeftAndRight;
    }

    public Rect getDetctAreaRect() {
        int offset = 0;
        //if(previewHeight > 0){
        //    offset = getHeight() - previewHeight;
        //}
        return new Rect(borderLeftAndRight[0],borderLeftAndRight[1]+offset,borderLeftAndRight[2],borderLeftAndRight[3]+offset);
    }
}
