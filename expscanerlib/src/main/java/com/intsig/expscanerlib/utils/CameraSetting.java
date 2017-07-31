package com.intsig.expscanerlib.utils;

import android.content.Context;
import android.hardware.Camera;
import android.widget.Toast;

import com.intsig.expscanerlib.R;

import java.util.List;

/**
 * Created by Android Studio.
 * ProjectName: ExpScannerSDKCaller
 * Author: haozi
 * Date: 2017/7/31
 * Time: 17:17
 */

public class CameraSetting {

    private Context context;

    private CameraSetting(Context context) {
        this.context = context;
    }

    private static CameraSetting single = null;

    /**
     * 静态工厂方法
     */
    public static CameraSetting getInstance(Context context) {
        if (single == null) {
            single = new CameraSetting(context);
        }
        return single;
    }

    /**
     * @Title: ${enclosing_method}
     * @Description: 打开闪光灯
     * @param camera 相机对象
     * @return void 返回类型
     * @throws
     */
    public void openCameraFlash(Camera camera) {
        if (camera == null){
            camera = Camera.open();
        }
        Camera.Parameters parameters = camera.getParameters();
        List<String> flashList = parameters.getSupportedFlashModes();
        if (flashList != null && flashList.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
        } else {
            Toast.makeText(context, context.getString(R.string.unsupportflash),Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * @Title: ${enclosing_method}
     * @Description: 关闭闪光灯
     * @param camera 相机对象
     * @return void 返回类型
     * @throws
     */
    public void closedCameraFlash(Camera camera) {
        if (camera == null){
            camera = Camera.open();
        }
        Camera.Parameters parameters = camera.getParameters();
        List<String> flashList = parameters.getSupportedFlashModes();
        if (flashList != null && flashList.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);
        } else {
            Toast.makeText(context, context.getString(R.string.unsupportflash),Toast.LENGTH_SHORT).show();
        }
    }

}
