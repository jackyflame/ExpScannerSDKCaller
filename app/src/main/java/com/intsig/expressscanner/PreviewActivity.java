/**
 * Project Name:IDCardScanCaller
 * File Name:ScanActivity.java
 * Package Name:com.intsig.idcardscancaller
 * Date:2016年3月15日下午2:14:46
 * Copyright (c) 2016, 上海合合信息 All Rights Reserved.
 */

package com.intsig.expressscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.graphics.Region;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.intsig.exp.sdk.ExpScannerCardUtil;
import com.intsig.exp.sdk.IRecogStatusListener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * ClassName:ScanActivity <br/>
 * Function: TODO ADD FUNCTION. <br/>
 * Reason: TODO ADD REASON. <br/>
 * Date: 2016年3月15日 下午2:14:46 <br/>
 *
 * @author guohua_xu
 */
public class PreviewActivity extends Activity implements
        Camera.PreviewCallback, Camera.AutoFocusCallback {
    private static final String TAG = "ScanActivity";

    public static final String EXTRA_KEY_APP_KEY = "EXTRA_KEY_APP_KEY";
    public static final String EXTRA_KEY_RESULT_DATA = "EXTRA_KEY_RESULT_DATA";
    public static final String EXTRA_KEY_RESULT_TYPE = "EXTRA_KEY_RESULT_TYPE";

    private DetectThread mDetectThread = null;
    private Preview mPreview = null;
    private Camera mCamera = null;
    private int numberOfCameras;

    // The first rear facing camera
    private int defaultCameraId;

    private float mDensity = 2.0f;

    private ExpScannerCardUtil expScannerCardUtil = null;

    private String mImageFolder = "/sdcard/idcardscan/";
    SoundClips.Player mSoundPlayer;
    private int mColorNormal = 0xff00ff00;
    private int mColorMatch = 0xffffffff;
    RelativeLayout rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDensity = getResources().getDisplayMetrics().density;
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // // Hide the window title.
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 隐藏当前Activity界面的导航栏, 隐藏后,点击屏幕又会显示出来.
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                ;
        decorView.setSystemUiVisibility(uiOptions);

        mImageFolder = this.getFilesDir().getPath();
        Log.e("mImageFolder", mImageFolder);
        File file = new File(mImageFolder);
        if (!file.exists()) {
            file.mkdirs();
        }

        /*************************** set a SurfaceView as the content of our activity.******START ***********************/

//		mPreview = new Preview(this);
//		setContentView(mPreview);

        /*************************** set a SurfaceView as the content of our activity.******START ***********************/

        mPreview = new Preview(this);

        float dentisy = getResources().getDisplayMetrics().density;
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(0xAA666666);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        root.addView(mPreview, lp);

        lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        lp.bottomMargin = (int) (50 * dentisy);

        setContentView(root);
        rootView = root;
        // 初始化预览界面左边按钮组
//		initButtonGroup();
        /*************************** Find the ID of the default camera******END ***********************/
        /*************************** Find the ID of the default camera******END ***********************/

        /*************************** Find the ID of the default camera******START ***********************/
        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();
        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                defaultCameraId = i;
            }
        }
        /*************************** Find the ID of the default camera******END ***********************/

        /*************************** Add mPreview Touch Listener******START ***********************/

        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mCamera != null) {
                    mCamera.autoFocus(null);
                }
                return false;
            }
        });
        /*************************** Add mPreview Touch Listener******END ***********************/

        /*************************** init recog appkey ******START ***********************/
        expScannerCardUtil = new ExpScannerCardUtil();
        Intent intent = getIntent();
        final String appkey = intent.getStringExtra(EXTRA_KEY_APP_KEY);

        int ret = -1;

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int ret = expScannerCardUtil.initRecognizer(getApplication(),
                        appkey);
                return ret;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != 0) {

                    /**
                     * 101 包名错误, 授权APP_KEY与绑定的APP包名不匹配； 102
                     * appKey错误，传递的APP_KEY填写错误； 103 超过时间限制，授权的APP_KEY超出使用时间限制；
                     * 104 达到设备上限，授权的APP_KEY使用设备数量达到限制； 201
                     * 签名错误，授权的APP_KEY与绑定的APP签名不匹配； 202 其他错误，其他未知错误，比如初始化有问题；
                     * 203 服务器错误，第一次联网验证时，因服务器问题，没有验证通过； 204
                     * 网络错误，第一次联网验证时，没有网络连接，导致没有验证通过； 205
                     * 包名/签名错误，授权的APP_KEY与绑定的APP包名和签名都不匹配；
                     */
                    new AlertDialog.Builder(PreviewActivity.this)
                            .setTitle("初始化失败")
                            .setMessage(
                                    "识别库初始失败,请检查 app key是否正确\n,错误码:" + result)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            finish();
                                        }
                                    })

                            .create().show();
                }
            }
        }.execute();
        mSoundPlayer = SoundClips.getPlayer(PreviewActivity.this);

        /*************************** init recog appkey ******END ***********************/

    }

    boolean mNeedInitCameraInResume = false;

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mSoundPlayer.play(SoundClips.PICTURE_BEGIN);

            mCamera = Camera.open(defaultCameraId);// open the default camera
        } catch (Exception e) {
            e.printStackTrace();
            showFailedDialogAndFinish();
            return;
        }
        /********************************* preview是自定义的viewgroup 继承了surfaceview,将相机和surfaceview 通过holder关联 ***********************/
        mPreview.setCamera(mCamera);
        /********************************* 设置显示的图片和预览角度一致 ***********************/
        setDisplayOrientation();
        try {
            /********************************* 对surfaceview的PreviewCallback的 callback监听，回调onPreviewFrame ***********************/
            mCamera.setOneShotPreviewCallback(this);
        } catch (Exception e) {
            e.printStackTrace();

        }
        /*************************** 当按power键后,再回到程序,surface 不会调用created/changed,所以需要主动初始化相机参数******START ***********************/
        if (mNeedInitCameraInResume) {
            mPreview.surfaceCreated(mPreview.mHolder);
            mPreview.surfaceChanged(mPreview.mHolder, 0,
                    mPreview.mSurfaceView.getWidth(),
                    mPreview.mSurfaceView.getHeight());
            mHandler.sendEmptyMessageDelayed(100, 100);
        }
        mNeedInitCameraInResume = true;
        /********************************* END ***********************/

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            Camera camera = mCamera;
            mCamera = null;
            camera.setOneShotPreviewCallback(null);
            mPreview.setCamera(null);
            camera.release();
            camera = null;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expScannerCardUtil != null) {
            expScannerCardUtil.releaseRecognizer();
        }
        if (mDetectThread != null) {
            mDetectThread.stopRun();
        }
        if (mSoundPlayer != null) {
            mSoundPlayer.release();
            mSoundPlayer = null;
        }
        mHandler.removeMessages(MSG_AUTO_FOCUS);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Size size = camera.getParameters().getPreviewSize();
        if (mDetectThread == null) {
            mDetectThread = new DetectThread();
            mDetectThread.start();
            /********************************* 自动对焦的核心 启动handler 来进行循环对焦 ***********************/
            mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 200);

        }

        /********************************* 向预览线程队列中 加入预览的 data 分析是否ismatch ***********************/
        // Log.e("onPreviewFrame size", "width" + size.width + "h:" +
        // size.height);
        mDetectThread.addDetect(data, size.width, size.height);
    }

    private void showFailedDialogAndFinish() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.fail_to_contect_camcard)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                            }
                        }).create().show();
    }

    private void resumePreviewCallback() {
        if (mCamera != null) {
            mCamera.setOneShotPreviewCallback(this);
        }
    }

    /**
     * 功能：将显示的照片和预览的方向一致
     */
    private void setDisplayOrientation() {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(defaultCameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result = (info.orientation - degrees + 360) % 360;
        mCamera.setDisplayOrientation(result);
        /**
         * 注释原因：因为FOCUS_MODE_CONTINUOUS_PICTURE 不一定兼容所有手机
         * 小米4华为mate8对焦有问题，现在考虑用定时器来实现自动对焦
         */

        // Camera.Parameters params = mCamera.getParameters();
        // String focusMode = Parameters.FOCUS_MODE_AUTO;
        // if (!TextUtils.equals("samsung", android.os.Build.MANUFACTURER)) {
        // focusMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        // }
        // Log.d("focusMode", focusMode);
        //
        // if (!isSupported(focusMode, params.getSupportedFocusModes())) {
        // // For some reasons, the driver does not support the current
        // // focus mode. Fall back to auto.
        // Log.d(" not isSupported", "not");
        //
        // if (isSupported(Parameters.FOCUS_MODE_AUTO,
        // params.getSupportedFocusModes())) {
        // focusMode = Parameters.FOCUS_MODE_AUTO;
        // } else {
        // focusMode = params.getFocusMode();
        // }
        // Log.d(" not isSupported", focusMode);
        //
        // }
        // params.setFocusMode(focusMode);
        // mCamera.setParameters(params);
        // if (!TextUtils.equals(focusMode,
        // Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
        // Log.d("FOCUS_MODE_CONTINUOUS_PICTURE", "not");
        // mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 2000);
        // }
    }

    public boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    private static final int MSG_AUTO_FOCUS = 100;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_AUTO_FOCUS) {
                autoFocus();
                mHandler.removeMessages(MSG_AUTO_FOCUS);
                // 两秒后进行聚焦
                mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 2000);
            }
        }

        ;
    };

    private void autoFocus() {
        if (mCamera != null) {
            try {
                mCamera.autoFocus(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    boolean isFocus = false;

    /**
     * 功能：对焦后的回调，每次返回bool值，如果对焦成功延时2秒对焦，如果失败继续对焦
     */
    // @SuppressWarnings("deprecation")
    // AutoFocusCallback focusCallback = new AutoFocusCallback() {
    //
    // @Override
    // public void onAutoFocus(boolean success, Camera camera) {
    // // Log.d("lz", "success==" + success);
    //
    // if (success) {
    // if (camera != null) {
    // isFocus = true;
    // mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 200);
    //
    // }
    // } else {
    // if (camera != null) {
    // isFocus = false;
    // mHandler.sendEmptyMessage(MSG_AUTO_FOCUS);
    // // Log.d("lz", "isFocus==" + isFocus);
    //
    // }
    // }
    //
    // }
    // };

    boolean isVertical = true;
    int[] borderLeftAndRight = new int[4];// 预览框的左右坐标---竖屏的时候

    /**
     *
     *
     * @param newWidth
     * @param newHeight
     * @return
     */
    public Map<String, Float> getPositionWithArea(int newWidth, int newHeight,
                                                  float scale, float scaleH) {
        float left = 0, top = 0, right = 0, bottom = 0;

        float borderWidth = newWidth;
        float borderHeight = newHeight / 10;
        // 注意：机打号的预览框高度设置建议是 屏幕高度的1/10,宽度 尽量与屏幕同宽
        Log.e("getPositionWithArea",
                "scale:" + scale + ",scaleH:" + scaleH);
        Map<String, Float> map = new HashMap<String, Float>();
        if (isVertical) {// vertical
            float dis = 0;

            left = newWidth * dis;
            right = newWidth - left;
            borderWidth = newWidth - 2 * left;// 调整预览框的 宽度
            // 注释的部分 打开就是将预览框放置中心位置

            top = newHeight / 2 - borderHeight / 2;
            bottom = newHeight / 2 + borderHeight / 2;

//			top = 400 * scale;
//			bottom = 400 * scale + borderHeight;
            borderLeftAndRight[0] = (int) top;
            borderLeftAndRight[1] = (int) left;

            borderLeftAndRight[2] = (int) bottom;

            borderLeftAndRight[3] = (int) right;

        }
        map.put("left", left);
        map.put("right", right);

        map.put("top", top);

        map.put("bottom", bottom);

        return map;

    }

    private Set<String> setResultSet = new HashSet<String>();

    // thread to detect and recognize.

    /**
     * 功能：将每一次预览的data 存入ArrayBlockingQueue 队列中，然后依次进行ismatch的验证，如果匹配就会就会进行进一步的识别
     * 注意点： 1.其中 控制预览框的位置大小，需要
     */

    public void showView(final String result, final String time) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                StringBuffer sb = new StringBuffer();
                for (String s : setResultSet) {
                    sb.append(s + "  ");
                }
                mResultValueAll.setText("当前识别结果集：" + sb.toString());

                mResultValue.setText("当前识别结果：" + result + "耗时：" + time);
            }

        });

    }

    String lastRecgResultString = null;

    private class DetectThread extends Thread {
        private ArrayBlockingQueue<byte[]> mPreviewQueue = new ArrayBlockingQueue<byte[]>(1);
        private int width;
        private int height;

        public void stopRun() {
            addDetect(new byte[]{0}, -1, -1);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    byte[] data = mPreviewQueue.take();// block here, if no data
                    // in the queue.
                    if (data.length <= 1) {// quit the thread, if we got special
                        // byte array put by stopRun().
                        return;
                    }
                    float left, top, right, bottom;
                    int newWidth = height;
                    int newHeight = width;
                    // Log.e(" newWidth newHeight", newWidth + "," + newHeight);
                    Map<String, Float> map = getPositionWithArea(newWidth,
                            newHeight, 1, 1);
                    left = map.get("left");
                    right = map.get("right");
                    top = map.get("top");
                    bottom = map.get("bottom");

                    // the (left, top, right, bottom) is base on preview image's
                    // coordinate. that's different with ui coordinate.
                    /********************************* 通过底册api 将预览的数据 还有证件的坐标位置 获取当前一帧证件的4个点坐标的数组 ***********************/
                    // borderLeftAndRight[0]=0;
                    // borderLeftAndRight[1]=0;
                    // borderLeftAndRight[2]=width;
                    // borderLeftAndRight[3]=height;


                    final long starttime = System.currentTimeMillis();
                    expScannerCardUtil.recognizeExp(data, width, height,
                            borderLeftAndRight, new IRecogStatusListener() {

                                @Override
                                public void onRecognizeExp(String result, int type) {
                                    Log.e("DetectExpressBillBarCodeAndNumberROI","true");
                                    /**
                                     * 一帧数据立马返回结果
                                     */
                                    // mSoundPlayer
                                    // .play(SoundClips.PICTURE_COMPLETE);
                                    // setResultSet.add(result);
                                    // showView(result);s
                                    // // showResult(result, type);
                                    // resumePreviewCallback();

                                    // TODO Auto-generated method stub
                                    // Log.e("result", result);
                                    // mSoundPlayer
                                    // .play(SoundClips.PICTURE_COMPLETE);
                                    //
                                    // showResult(result, type);
                                    // Log.e("type", type + "");
                                    /**
                                     * 连续两帧数据一样才返回结果
                                     */
                                    if (lastRecgResultString == null) {
                                        //
                                        showView("lastRecgResultString:null,"
                                                + "result:" + result, "");

                                        lastRecgResultString = result;
                                        resumePreviewCallback();
                                    } else {
                                        // showView("lastRecgResultString:"+
                                        // lastRecgResultString+",result:"+
                                        // result);

                                        if (result.equals(lastRecgResultString)) {

                                            long endtime = System.currentTimeMillis();


                                            mSoundPlayer
                                                    .play(SoundClips.PICTURE_COMPLETE);
                                            setResultSet.add(result);
                                            showView(result, (endtime - starttime) + "ms");
                                            // showResult(result, type);
                                            lastRecgResultString = result;
                                            resumePreviewCallback();
                                        } else {
                                            lastRecgResultString = result;
                                            resumePreviewCallback();
                                        }

                                    }

                                    // showResult(result,type);
                                }

                                @Override
                                public void onRecognizeError(int arg0) {
                                    Log.e("DetectExpressBillBarCodeAndNumberROI",
                                            "false");

                                    // TODO Auto-generated method stub
                                    resumePreviewCallback();
                                }
                            });

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int continue_match_time = 0;

        /**
         * 当前预览帧的 证件四个点的坐标 和 预览框的证件4个点的坐标 校验，在一定范围内认定校验成功
         * 注意点：其中120是多次验证的值，没有其他理由校验比较稳定，这个值可以自己尝试改变
         *
         * @param left
         * @param top
         * @param right
         * @param bottom
         * @param qua
         * @return
         */
        public boolean isMatch(int left, int top, int right, int bottom,
                               int[] qua) {
            int dif = 120;
            int num = 0;

            if (Math.abs(left - qua[6]) < dif && Math.abs(top - qua[7]) < dif) {
                num++;
            }
            if (Math.abs(right - qua[0]) < dif && Math.abs(top - qua[1]) < dif) {
                num++;
            }
            if (Math.abs(right - qua[2]) < dif
                    && Math.abs(bottom - qua[3]) < dif) {
                num++;
            }
            if (Math.abs(left - qua[4]) < dif
                    && Math.abs(bottom - qua[5]) < dif) {
                num++;
            }
            System.out.println("inside " + Arrays.toString(qua) + " <>" + left
                    + ", " + top + ", " + right + ", " + bottom + "           "
                    + num);
            if (num > 2) {
                continue_match_time++;
                if (continue_match_time >= 1)
                    return true;
            } else {
                continue_match_time = 0;
            }
            return false;
        }

        public void addDetect(byte[] data, int width, int height) {
            if (mPreviewQueue.size() == 1) {
                mPreviewQueue.clear();
            }
            mPreviewQueue.add(data);
            this.width = width;
            this.height = height;
        }

        private void showResult(String result, int type) {
            // Log.e("result:", result);

            Intent intent = new Intent(PreviewActivity.this,
                    RecogResultActivity.class);
            intent.putExtra(PreviewActivity.EXTRA_KEY_RESULT_DATA, result);
            intent.putExtra(PreviewActivity.EXTRA_KEY_RESULT_TYPE, type);

            startActivity(intent);
            finish();
        }
    }

    /**
     * A simple wrapper around a Camera and a SurfaceView that renders a
     * centered preview of the Camera to the surface. We need to center the
     * SurfaceView because not all devices have cameras that support preview
     * sizes at the same aspect ratio as the device's display.
     */
    private TextView mResultValue = null;
    ;
    private TextView mResultValueAll = null;
    ;

    private class Preview extends ViewGroup implements SurfaceHolder.Callback {
        private final String TAG = "Preview";
        private SurfaceView mSurfaceView = null;
        private SurfaceHolder mHolder = null;
        private Size mPreviewSize = null;
        private List<Size> mSupportedPreviewSizes = null;
        private Camera mCamera = null;
        private DetectView mDetectView = null;
        private TextView mInfoView = null;
        private TextView mCopyRight = null;
        ;

        public Preview(Context context) {
            super(context);
            /*********************************
             * 自定义viewgrop上添加SurfaceView 然后对应的其他ui DetectView是自定义的预览框
             *
             * ***********************/

            mSurfaceView = new SurfaceView(context);
            addView(mSurfaceView);

            mInfoView = new TextView(context);
            addView(mInfoView);

            mDetectView = new DetectView(context);
            addView(mDetectView);

            mCopyRight = new TextView(PreviewActivity.this);
            mCopyRight.setGravity(Gravity.CENTER);
            mCopyRight.setText(R.string.intsig_copyright);
            addView(mCopyRight);

            mResultValue = new TextView(PreviewActivity.this);
            mResultValue.setGravity(Gravity.CENTER);
            mResultValue.setText("");
            mResultValue.setTextColor(Color.RED);
            addView(mResultValue);

            mResultValueAll = new TextView(PreviewActivity.this);
            mResultValueAll.setGravity(Gravity.CENTER);
            mResultValueAll.setText("");
            mResultValueAll.setTextColor(Color.RED);
            addView(mResultValueAll);

            mHolder = mSurfaceView.getHolder();
            mHolder.addCallback(this);
        }

        public void setCamera(Camera camera) {
            mCamera = camera;
            if (mCamera != null) {
                mSupportedPreviewSizes = mCamera.getParameters()
                        .getSupportedPreviewSizes();
                requestLayout();
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // We purposely disregard child measurements because act as a
            // wrapper to a SurfaceView that centers the camera preview instead
            // of stretching it.
            final int width = resolveSize(getSuggestedMinimumWidth(),
                    widthMeasureSpec);
            final int height = resolveSize(getSuggestedMinimumHeight(),
                    heightMeasureSpec);
            setMeasuredDimension(width, height);

            if (mSupportedPreviewSizes != null) {
                int targetHeight = 720;
                if (width > targetHeight && width <= 1080)
                    targetHeight = width;
                mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes,
                        height, width, targetHeight);// 竖屏模式，寬高颠倒
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            if (changed && getChildCount() > 0) {
                final View child = getChildAt(0);

                final int width = r - l;
                final int height = b - t;

                int previewWidth = width;
                int previewHeight = height;
                // if (mPreviewSize != null) {
                // previewWidth = mPreviewSize.height;
                // previewHeight = mPreviewSize.width;
                // }

                // Center the child SurfaceView within the parent.
                if (width * previewHeight > height * previewWidth) {
                    final int scaledChildWidth = previewWidth * height
                            / previewHeight;
                    child.layout((width - scaledChildWidth) / 2, 0,
                            (width + scaledChildWidth) / 2, height);
                    mDetectView.layout((width - scaledChildWidth) / 2, 0,
                            (width + scaledChildWidth) / 2, height);
                } else {
                    final int scaledChildHeight = previewHeight * width
                            / previewWidth;
                    child.layout(0, (height - scaledChildHeight) / 2, width,
                            (height + scaledChildHeight) / 2);
                    mDetectView.layout(0, (height - scaledChildHeight) / 2,
                            width, (height + scaledChildHeight) / 2);
                }
                getChildAt(1).layout(l, t, r, b);

                mResultValueAll
                        .layout(l, (int) (b - 48 * 4 * mDensity),
                                (int) (r - 8 * mDensity),
                                (int) (b - 48 * 2 * mDensity));

                mResultValue.layout(l, (int) (b - 48 * 2 * mDensity),
                        (int) (r - 8 * mDensity), (int) (b - 48 * mDensity));
                mCopyRight.layout(l, (int) (b - 48 * mDensity),
                        (int) (r - 8 * mDensity), b);
            }
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, acquire the camera and tell it
            // where to draw.
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

        private Size getOptimalPreviewSize(List<Size> sizes, int w, int h,
                                           int targetHeight) {
            final double ASPECT_TOLERANCE = 0.2;
            double targetRatio = (double) w / h;
            if (sizes == null)
                return null;
            Size optimalSize = null;
            double minDiff = Double.MAX_VALUE;

            // Try to find an size match aspect ratio and size
            for (Size size : sizes) {
                // Log.e("size:", size.width+"...."+size.height);
//				 if(size.width==800&&size.height==480){
//				 return size;
//				 }

                double ratio = (double) size.width / size.height;
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                    continue;
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }

            // Cannot find the one match the aspect ratio, ignore the
            // requirement
            if (optimalSize == null) {
                minDiff = Double.MAX_VALUE;
                for (Size size : sizes) {
                    if (Math.abs(size.height - targetHeight) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
                    }
                }
            }
            return optimalSize;
        }

        // private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        // final double ASPECT_TOLERANCE = 0.1;
        // double targetRatio = (double) w / h;
        // if (sizes == null)
        // return null;
        // Size optimalSize = null;
        // double minDiff = Double.MAX_VALUE;
        //
        // int targetHeight = h;
        //
        // // Try to find an size match aspect ratio and size
        // for (Size size : sizes) {
        // // Log.e("size", size.width + "," + size.height);
        // // if(size.height==720&&size.width==1280)
        // // {optimalSize = size;
        // // break;
        // // }
        // double ratio = (double) size.width / size.height;
        // if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
        // continue;
        // if (Math.abs(size.height - targetHeight) < minDiff) {
        // optimalSize = size;
        // minDiff = Math.abs(size.height - targetHeight);
        // }
        // }
        //
        // // Cannot find the one match the aspect ratio, ignore the
        // // requirement
        // if (optimalSize == null) {
        // minDiff = Double.MAX_VALUE;
        // for (Size size : sizes) {
        // if (Math.abs(size.height - targetHeight) < minDiff) {
        // optimalSize = size;
        // minDiff = Math.abs(size.height - targetHeight);
        // }
        // }
        // }
        // // Log.e("optimalSize", optimalSize.width + "," +
        // // optimalSize.height);
        // return optimalSize;
        // }

        public void surfaceChanged(SurfaceHolder holder, int format, int w,
                                   int h) {
            if (mCamera != null) {
                // Now that the size is known, set up the camera parameters and
                // begin the preview.
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setRotation(0);
                parameters.setPreviewSize(mPreviewSize.width,
                        mPreviewSize.height);
                parameters.setPreviewFormat(ImageFormat.NV21);
                requestLayout();
                mDetectView.setPreviewSize(mPreviewSize.width,
                        mPreviewSize.height);
                mInfoView.setText("preview：" + mPreviewSize.width + ","
                        + mPreviewSize.height);

                // parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                //
                // // 找比例和preview一样，但大小最接近3M的picture size
                // List<Size> pictureSizes =
                // parameters.getSupportedPictureSizes();
                // int dif = Integer.MAX_VALUE;
                // Size optialSize = null;
                // for (Size s : pictureSizes) {
                // int tmp = Math.abs(s.width * s.height - 4000000);
                // // if (tmp < 0)
                // // continue;
                // if (tmp < dif) {
                // dif = tmp;
                // optialSize = s;
                // }
                // }
                // if (optialSize != null) {
                // parameters.setPictureSize(optialSize.width,
                // optialSize.height);
                // System.out.println("xxxxxxxxxxx setPictureSize " +
                // optialSize.width + "," + optialSize.height);
                // }
                // List<Integer> picForamts =
                // parameters.getSupportedPictureFormats();
                // boolean supportNV16 = false;
                // for (Integer pform : picForamts) {
                // if (pform == ImageFormat.NV16) {
                // supportNV16 = true;
                // break;
                // }
                // }
                // supportNV16 = false;// 目前要使用彩色图像
                // if (supportNV16)
                // parameters.setPictureFormat(ImageFormat.NV16);
                // else
                // // Philips 手机不支持 NV16 :(
                // parameters.setPictureFormat(ImageFormat.JPEG);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
            }
        }

        public void showBorder(int[] border, boolean match) {
            mDetectView.showBorder(border, match);
        }
    }

    /**
     * the view show bank card border.
     */
    private class DetectView extends View {
        private Paint paint = null;
        private int[] border = null;
        private String result = null;
        private boolean match = false;
        private int previewWidth;
        private int previewHeight;
        private Context context;

        // 蒙层位置路径
        Path mClipPath = new Path();
        RectF mClipRect = new RectF();
        float mRadius = 12;
        int mRectWidth = 9;
        float cornerSize = 30;// 4个角的大小
        float cornerStrokeWidth = 8;

        public void showBorder(int[] border, boolean match) {
            this.border = border;
            this.match = match;
            postInvalidate();
        }

        public DetectView(Context context) {
            super(context);
            paint = new Paint();
            this.context = context;
            paint.setColor(0xffff0000);

        }

        public void setPreviewSize(int width, int height) {
            this.previewWidth = width;
            this.previewHeight = height;

        }

        int drawCount = 0;

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            // upateClipRegion();
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

            Map<String, Float> map = getPositionWithArea(getWidth(),
                    getHeight(), scale, scaleH);
            left = map.get("left");
            right = map.get("right");
            top = map.get("top");
            bottom = map.get("bottom");
            // Log.e("aaa","scale"+scale+"left"+
            // left+"right"+right+"top"+top+"bottom"+bottom);
            mClipPath.reset();
            mClipRect.set(left, top, right, bottom);
            mClipPath.addRoundRect(mClipRect, mRadius, mRadius, Direction.CW);
        }

        @Override
        public void onDraw(Canvas c) {

            // Log.e("aaa", "previewHeight:" + previewHeight + "previewWidth:"
            // + previewWidth + "getWidth：" + getWidth() + "getHeight:"
            // + getHeight());
            // previewHeight:480previewWidth:720getWidth：1200getHeight:1830
            float scale = getWidth() / (float) previewHeight;
            float scaleH = getHeight() / (float) previewWidth;

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
            c.drawLine(mClipRect.left - strokeWidth / 2, mClipRect.top,
                    mClipRect.left + len, mClipRect.top, paint);
            c.drawLine(mClipRect.left, mClipRect.top, mClipRect.left,
                    mClipRect.top + len, paint);
            // 右上
            c.drawLine(mClipRect.right - len, mClipRect.top, mClipRect.right
                    + strokeWidth / 2, mClipRect.top, paint);
            c.drawLine(mClipRect.right, mClipRect.top, mClipRect.right,
                    mClipRect.top + len, paint);
            // 右下
            c.drawLine(mClipRect.right - len, mClipRect.bottom, mClipRect.right
                    + strokeWidth / 2, mClipRect.bottom, paint);
            c.drawLine(mClipRect.right, mClipRect.bottom - len,
                    mClipRect.right, mClipRect.bottom, paint);
            // 左下
            c.drawLine(mClipRect.left - strokeWidth / 2, mClipRect.bottom,
                    mClipRect.left + len, mClipRect.bottom, paint);
            c.drawLine(mClipRect.left, mClipRect.bottom - len, mClipRect.left,
                    mClipRect.bottom, paint);

            drawCount++;
            if (border != null) {
                paint.setStrokeWidth(3);
                int height = getWidth();

                c.drawLine(border[0] * scale, border[1] * scale, border[2]
                        * scale, border[3] * scale, paint);
                c.drawLine(border[2] * scale, border[3] * scale, border[4]
                        * scale, border[5] * scale, paint);
                c.drawLine(border[4] * scale, border[5] * scale, border[6]
                        * scale, border[7] * scale, paint);
                c.drawLine(border[6] * scale, border[7] * scale, border[0]
                        * scale, border[1] * scale, paint);

            }

            float left, top, right, bottom;

            Map<String, Float> map = getPositionWithArea(getWidth(),
                    getHeight(), scale, scaleH);
            left = map.get("left");
            right = map.get("right");
            top = map.get("top");
            bottom = map.get("bottom");

            // paint.setColor(0xAA666666);
            // paint.setStyle(Paint.Style.STROKE);
            // c.drawRect(left, top, right, bottom, paint);

            // 画动态的中心线
            paint.setColor(context.getResources().getColor(R.color.back_line_3));
            paint.setStrokeWidth(1);
            c.drawLine(left, top + (bottom - top) / 2, right, top
                    + (bottom - top) / 2, paint);

            // if (drawCount % 2 == 1) {
            // paint.setColor(context.getResources().getColor(
            // R.color.back_line_3));
            // paint.setStrokeWidth(1);
            //
            // c.drawLine(left, top + (bottom - top) / 2, right, top
            // + (bottom - top) / 2, paint);
            //
            // invalidate();
            // } else {
            //
            // try {
            // Thread.sleep(100);
            // } catch (InterruptedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            // paint.setStrokeWidth(1);
            //
            // paint.setColor(context.getResources().getColor(
            // R.color.back_line_1));
            // c.drawLine(left, top + (bottom - top) / 2, right, top
            // + (bottom - top) / 2, paint);
            // invalidate();
            // }

        }
    }

    @Override
    public void onAutoFocus(boolean arg0, Camera arg1) {
        // TODO Auto-generated method stub

    }


    /**
     * 初始化预览界面左边按钮组，可以选择正反面识别 正面识别 反面识别 注：如果客户想要自定义预览界面，可以参考
     * initButtonGroup中的添加方式
     */
    private void initButtonGroup() {
        int width = getResources().getDisplayMetrics().widthPixels;
        float density = getResources().getDisplayMetrics().density;

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);


//		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
//
//		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        // **********************************添加动态的布局
        LayoutInflater inflater = getLayoutInflater();

        View view = inflater.inflate(R.layout.cui_button_group, null);

//		left = (TabButton) view.findViewById(R.id.btn_left);
//		left.setOnClickListener(this);
//
//		center = (TabButton) view.findViewById(R.id.btn_center);
//		center.setOnClickListener(this);
//
//		right = (TabButton) view.findViewById(R.id.btn_right);
//		right.setOnClickListener(this);
//
//		resetState(R.id.btn_left);
        rootView.addView(view, lp);
    }

}
