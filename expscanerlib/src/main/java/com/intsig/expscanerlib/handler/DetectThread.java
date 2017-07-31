package com.intsig.expscanerlib.handler;

import com.intsig.exp.sdk.ExpScannerCardUtil;
import com.intsig.exp.sdk.IRecogStatusListener;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Android Studio.
 * ProjectName: ExpScannerSDKCaller
 * Author: haozi
 * Date: 2017/7/31
 * Time: 10:44
 */

public class DetectThread extends Thread {

    private static final String TAG = "DetectThread";

    private ArrayBlockingQueue<byte[]> mPreviewQueue = new ArrayBlockingQueue<>(1);
    private int width;
    private int height;

    private ExpScannerCardUtil expScannerCardUtil = null;
    private int[] border;
    private IRecogStatusListener recogStatusListener;

    public DetectThread(ExpScannerCardUtil expScannerCardUtil, int[] border, IRecogStatusListener recogStatusListener) {
        this.expScannerCardUtil = expScannerCardUtil;
        this.border = border;
        this.recogStatusListener = recogStatusListener;
    }

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
                /********************************* 通过底册api 将预览的数据 还有证件的坐标位置 获取当前一帧证件的4个点坐标的数组 ***********************/
                expScannerCardUtil.recognizeExp(data, width, height, border, recogStatusListener);
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
