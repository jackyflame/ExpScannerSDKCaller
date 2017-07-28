package com.intsig.expscanerlib.ui;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.util.List;

/**
 * Created by Android Studio.
 * ProjectName: ExpScannerSDKCaller
 * Author: haozi
 * Date: 2017/7/26
 * Time: 17:10
 */
public class Preview extends ViewGroup implements SurfaceHolder.Callback{

    private final String TAG = "Preview";
    /**当前尺寸*/
    private Camera.Size mPreviewSize = null;
    /**摄像头支持的分辨率*/
    private List<Camera.Size> mSupportedPreviewSizes = null;
    /**摄像头引用*/
    private Camera mCamera = null;

    /**扫描区域*/
    private DetectView mDetectView = null;

    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mHolder = null;

    public Preview(Context context) {
        super(context);
        initView(context);
    }

    public Preview(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public Preview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && mSurfaceView != null) {
            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                mSurfaceView.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                mSurfaceView.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
            }

            Rect rect = new Rect();
            mDetectView.getGlobalVisibleRect(rect);
            mDetectView.setClipRect(rect);
        }
    }

    private void initView(Context context){
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }

    public void setDetectView(DetectView detectView) {
        this.mDetectView = detectView;
        if(mDetectView != null){
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
            mDetectView.setLayoutParams(layoutParams);
        }
    }

    public SurfaceHolder getHolder() {
        return mHolder;
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    public int[] getDetctArea() {
        return mDetectView.getDetctArea();
    }

    public Rect getDetctAreaRect() {
        return mDetectView.getDetctAreaRect();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(),widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            int targetHeight = 720;
            if (width > targetHeight && width <= 1080){
                targetHeight = width;
            }
            // 竖屏模式，寬高颠倒
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes,height, width, targetHeight);
            if(mDetectView != null){
                mDetectView.setPreviewSize(mPreviewSize.width,mPreviewSize.height);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()",
                    exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h,int targetHeight) {
        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;
        if (sizes == null){
            return null;
        }
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCamera != null) {
            // Now that the size is known, set up the camera parameters and
            // begin the preview.
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(0);
            parameters.setPreviewSize(mPreviewSize.width,mPreviewSize.height);
            parameters.setPreviewFormat(ImageFormat.NV21);

            int maxZoom = parameters.getMaxZoom();
            if (parameters.isZoomSupported()) {
                int zoom = (maxZoom * 3) / 10;
                if (zoom < maxZoom && zoom > 0) {
                    parameters.setZoom(zoom);
                }
            }
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);

            requestLayout();

            if(mDetectView != null){
                mDetectView.setPreviewSize(mPreviewSize.width,mPreviewSize.height);
            }

            Log.i(TAG, "surfaceChanged->preview：" + mPreviewSize.width + "," + mPreviewSize.height);

            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }
    }

    public void showBorder(int[] border, boolean match) {
        mDetectView.showBorder(border, match);
    }
}
