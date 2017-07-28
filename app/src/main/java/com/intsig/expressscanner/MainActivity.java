package com.intsig.expressscanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import com.intsig.expscanerlib.ui.ScanActivity;

public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = "MainActivity";
	public static final String APP_KEY = "6a7c9a4580b10338a47e29bf91-Vagfvt";// 替换您申请的合合信息授权提供的APP_KEY;20170701 
//	public static final String APP_KEY = "2bAJD52WRNNVdEf1aFrHNT50";
	private static final int REQ_CODE_CAPTURE = 100;
	
		public static boolean boolCheckAppKey = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

	
		  
		

        if (!boolCheckAppKey) {
			useCamare();
		} else {
			setContentView(R.layout.main_activity);
			findViewById(R.id.btn_use_camare_module).setOnClickListener(this);
		     
			
			
			
//			ExpScannerCardUtil expScannerCardUtil = new ExpScannerCardUtil();
//		final String APPKEY_Online = "2a9A9JMD47XFtPJUMQBMBeSD";
//
//			CheckAppKey.showCustomizeDialog(MainActivity.this, APPKEY_Online,
//					APP_KEY, APP_KEY, new AppkeyCallBack() {
//
//						@Override
//						public void successUpdateUi(int result) {
//							// TODO Auto-generated method stub
//							Log.d("init result:", "授权成功->" + result);
//							Toast.makeText(MainActivity.this,
//									"授权成功->" + result + "\nMsg:"+CommonUtil.commentMsg(result), Toast.LENGTH_SHORT)
//									.show();
//						}
//
//						@Override
//						public void releaseSdk() {
//							// TODO Auto-generated method stub
//							expScannerCardUtil.releaseRecognizer();
//						}
//
//						@Override
//						public int initSdk(String appkey) {
//							// TODO Auto-generated method stub
//							
//							return expScannerCardUtil.initRecognizer(
//									getApplication(), appkey);
//						}
//
//						@Override
//						public void failedUpdateUi(int result) {
//							// TODO Auto-generated method stub
//							Log.d("init result:", "授权失败->" + result);
//							Toast.makeText(MainActivity.this,
//									"授权失败->" + result + "\nMsg:"+CommonUtil.commentMsg(result),
//									Toast.LENGTH_SHORT).show();
//						}
//					});
//
		}
	}

	public void useCamare(){
		// 在PreviewActivity中自定义相机拍摄模块，然后直接调用SDK中的身份证预览方法进行识别

		
		Intent intent = new Intent(this, ScanActivity.class);
//		Intent intent = new Intent(this, PreviewActivity.class);
		// 合合信息授权提供的APP_KEY
		intent.putExtra(ScanActivity.EXTRA_KEY_APP_KEY, APP_KEY);
        startActivityForResult(intent, REQ_CODE_CAPTURE);
	}
	
	@Override
	public void onClick(View view) {
		int id = view.getId();

		if (id == R.id.btn_use_camare_module) {

			useCamare();
		}
	}
	 @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        super.onActivityResult(requestCode, resultCode, data);
	        if(resultCode == RESULT_OK && requestCode == REQ_CODE_CAPTURE){
	           

				if (!boolCheckAppKey) {
					finish();
				}
	        } else if (resultCode == RESULT_CANCELED && requestCode == REQ_CODE_CAPTURE) {
	          //识别失败或取消
	            Log.d(TAG, "识别失败或取消");
	            
	            
	        	if (!boolCheckAppKey) {
					finish();
				}
	        }
	    }
}
