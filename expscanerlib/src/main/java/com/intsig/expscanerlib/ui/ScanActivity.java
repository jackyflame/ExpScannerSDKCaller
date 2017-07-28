/**
 * Project Name:IDCardScanCaller
 * File Name:ScanActivity.java
 * Package Name:com.intsig.idcardscancaller
 * Date:2016年3月15日下午2:14:46
 * Copyright (c) 2016, 上海合合信息 All Rights Reserved.
 */

package com.intsig.expscanerlib.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.intsig.exp.sdk.ExpScannerCardUtil;
import com.intsig.exp.sdk.IRecogStatusListener;
import com.intsig.expscanerlib.R;
import com.intsig.expscanerlib.utils.SoundClips;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * ClassName:ScanActivity <br/>
 * Function: TODO ADD FUNCTION. <br/>
 * Reason: TODO ADD REASON. <br/>
 */
public class ScanActivity extends Activity implements Camera.PreviewCallback, Camera.AutoFocusCallback {

    private static final String TAG = "ScanActivity";
    public static final String EXTRA_KEY_APP_KEY = "EXTRA_KEY_APP_KEY";
    private static final int MSG_AUTO_FOCUS = 100;

    private DetectThread mDetectThread = null;
    private Preview mPreview = null;
    private Camera mCamera = null;
    private int numberOfCameras;

    // The first rear facing camera
    private int defaultCameraId;
    private ExpScannerCardUtil expScannerCardUtil = null;
    private SoundClips.Player mSoundPlayer;

    private ImageView img_rst;
    private TextView txv_rst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        mPreview = (Preview) findViewById(R.id.preview_scan);
        mPreview.setDetectView((DetectView) findViewById(R.id.detect_scan));
        img_rst = (ImageView) findViewById(R.id.img_rst);
        txv_rst = (TextView) findViewById(R.id.txv_rst);

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
        initEngin();
    }

    private void initEngin(){
        /*************************** init recog appkey ******START ***********************/
        expScannerCardUtil = new ExpScannerCardUtil();
        Intent intent = getIntent();
        final String appkey = intent.getStringExtra(EXTRA_KEY_APP_KEY);
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int ret = expScannerCardUtil.initRecognizer(getApplication(),appkey);
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
                    new AlertDialog.Builder(ScanActivity.this)
                            .setTitle("初始化失败")
                            .setMessage("识别库初始失败,请检查 app key是否正确\n,错误码:" + result)
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
        mSoundPlayer = SoundClips.getPlayer(ScanActivity.this);
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
            mPreview.surfaceCreated(mPreview.getHolder());
            mPreview.surfaceChanged(mPreview.getHolder(), 0, mPreview.getSurfaceView().getWidth(), mPreview.getSurfaceView().getHeight());
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
        mDetectThread.addDetect(data, size.width, size.height);
    }

    private void showFailedDialogAndFinish() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.fail_to_contect_camcard)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(defaultCameraId, info);
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
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_AUTO_FOCUS) {
                autoFocus();
                mHandler.removeMessages(MSG_AUTO_FOCUS);
                // 两秒后进行聚焦
                mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 2000);
            }
        }
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

    private Set<String> setResultSet = new HashSet<>();

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
                Log.i(TAG, "当前识别结果：" + result + "耗时：" + time);
                //mResultValueAll.setText("当前识别结果集：" + sb.toString());
                txv_rst.setText("当前识别结果：" + result + "耗时：" + time);
            }
        });
    }

    public void showCheckView(final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                img_rst.setImageBitmap(bitmap);
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
                    // block here, if no data in the queue.
                    byte[] data = mPreviewQueue.take();
                    // quit the thread, if we got special  byte array put by stopRun().
                    if (data.length <= 1) {
                        return;
                    }
                    // the (left, top, right, bottom) is base on preview image's coordinate. that's different with ui coordinate.
                    /********************************* 通过底册api 将预览的数据 还有证件的坐标位置 获取当前一帧证件的4个点坐标的数组 ***********************/
                    final long starttime = System.currentTimeMillis();

                    //Rect clipRect = new Rect(borderLeftAndRight[0],borderLeftAndRight[1],borderLeftAndRight[2],borderLeftAndRight[3]);
                    //Rect clipRect = mPreview.getDetctAreaRect();
                    //if(clipRect.height() > 0 && clipRect.width() > 0){
                    //    showCheckView(ImageUtils.makeCropedGrayBitmap(data.clone(),width,height,90,clipRect));
                    //}

                    expScannerCardUtil.recognizeExp(data, width, height, mPreview.getDetctArea(),
                            new IRecogStatusListener() {
                                @Override
                                public void onRecognizeExp(String result, int type) {
                                    Log.e(TAG, "DetectExpressBillBarCodeAndNumberROI:true");
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
                                        showView("lastRecgResultString:null,"+ "result:" + result, "");
                                        lastRecgResultString = result;
                                        resumePreviewCallback();
                                    } else {
                                        // showView("lastRecgResultString:"+
                                        // lastRecgResultString+",result:"+
                                        // result);
                                        if (result.equals(lastRecgResultString)) {
                                            long endtime = System.currentTimeMillis();
                                            mSoundPlayer.play(SoundClips.PICTURE_COMPLETE);
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
                                    Log.e(TAG,"DetectExpressBillBarCodeAndNumberROI:false");
                                    // TODO Auto-generated method stub
                                    resumePreviewCallback();
                                }
                            });

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void addDetect(byte[] data, int width, int height) {
            if (mPreviewQueue.size() == 1) {
                mPreviewQueue.clear();
            }
            mPreviewQueue.add(data);
            this.width = width;
            this.height = height;
        }
    }

    @Override
    public void onAutoFocus(boolean arg0, Camera arg1) {}

}
