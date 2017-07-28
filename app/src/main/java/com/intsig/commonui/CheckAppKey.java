package com.intsig.commonui;

import java.lang.reflect.Field;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.intsig.expressscanner.R;


public class CheckAppKey {
	private static String APPKEY_Online, APPKEY, APPKEY_DEFAULT;

	public static void showCustomizeDialog(final Activity context,
			String appkeyonline, String appkey, String appkeyDefault,
			
			final AppkeyCallBack appkeyCallBack) {
		APPKEY_Online = appkeyonline;
		APPKEY = appkey;
		APPKEY_DEFAULT = appkeyDefault;
		/*
		 * @setView 装入自定义View ==> R.layout.dialog_customize
		 * 由于dialog_customize.xml只放置了一个EditView，因此和图8一样
		 * dialog_customize.xml可自定义更复杂的View
		 */
		AlertDialog.Builder customizeDialog = new AlertDialog.Builder(context);
		final View dialogView = LayoutInflater.from(context).inflate(
				R.layout.cui_dialog_customize, null);
		final EditText edit_text = (EditText) dialogView
				.findViewById(R.id.edit_text_id);
		final TextView online_text_id = (TextView) dialogView
				.findViewById(R.id.online_text_id);
		online_text_id.setText("在线key:" + APPKEY_Online);
		final TextView outline_text_id = (TextView) dialogView
				.findViewById(R.id.outline_text_id);
		outline_text_id.setText("离线key:" + APPKEY);
		final TextView date_text_id = (TextView) dialogView
				.findViewById(R.id.date_text_id);
		date_text_id.setText("离线有效期:" + CheckAppKey.getExpireDate(APPKEY));

		edit_text.setText(APPKEY_DEFAULT);
		// edit_text.setHint(APPKEY);
		customizeDialog.setTitle("AppKey验证");
		customizeDialog.setView(dialogView);

		// final IDCardScanSDK mIDCardScanSDK = new IDCardScanSDK();
		customizeDialog.setPositiveButton("确定",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 获取EditView中的输入内容
						// canCloseDialog(dialog, false);
						// mIDCardScanSDK.release();
						appkeyCallBack.releaseSdk();
						doRecogInit(context, dialog, edit_text.getText()
								.toString(),
								appkeyCallBack);
					}
				});
		customizeDialog.setNegativeButton("取消",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						context.finish();
					}
				});
		customizeDialog.setCancelable(false);
		AlertDialog dialog = customizeDialog.create();
		dialog.setCanceledOnTouchOutside(false);
		customizeDialog.show();
	}

	// 关键部分在这里
	private void canCloseDialog(DialogInterface dialogInterface, boolean close) {
		try {
			Field field = dialogInterface.getClass().getSuperclass()
					.getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialogInterface, close);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 做识别工作放这里
	 */
	private static void doRecogInit(final Activity context,
			final DialogInterface dialog, final String appkey,
		
			final AppkeyCallBack appkeyCallBack) {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected void onPreExecute() {

				super.onPreExecute();
			}

			@Override
			protected Integer doInBackground(Void... params) {
				final int[] results = new int[1];

				int code = appkeyCallBack.initSdk(appkey);

				// int code =
				// mIDCardScanSDK.initIDCardScan(context.getApplicationContext(),
				// appkey);
				Log.d(context.toString(), "code=" + code);

				results[0] = code;

				return results[0];
			}

			protected void onPostExecute(Integer result) {
				if (result == 0) {// 授权成功
					Log.d(context.toString(), "授权成功->" + result);
					
					appkeyCallBack.successUpdateUi(result);
					
				
				} else {// 授权失败
					Log.d(context.toString(), "授权失败-->" + result 
							);
					appkeyCallBack.failedUpdateUi(result);
					
				
					showCustomizeDialog(context, APPKEY_Online, APPKEY, appkey,
							appkeyCallBack);
				}
			}
		}.execute();
	}

	public static void main(String args[]) {
		String key = "8e5fa9ba23cff6632f58299a11-Vagfvt";

		System.out.println("vendorId:" + getVenodrId(key));
		System.out.println("Expire:" + getExpireDate(key));
	}

	public static String getExpireDate(String key) {
		try {
			String tmp = key.substring(20, 25);
			int date = Integer.parseInt(tmp, 16) + 20000000;
			if (date < 20150000)
				return null;
			return date + "";
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return "error";
	}

	public static String getVenodrId(String key) {
		int index = key.lastIndexOf('-');
		if (index > 0) {
			try {
				String tmp = key.substring(index + 1);
				char data[] = new char[tmp.length()];
				for (int i = 0; i < data.length; i++) {
					data[i] = convert(tmp.charAt(i));
				}
				return new String(data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	static char convert(char c) {
		int n = c;

		if (c >= 'a' && c <= 'z') {
			n = c + 13;
			if (n > 'z') {
				n = n - 'z' + 'a' - 1;
			}
		} else if (c >= 'A' && c <= 'Z') {
			n = c + 13;
			if (n > 'Z') {
				n = n - 'Z' + 'A' - 1;
			}
		}
		return (char) n;
	}

	public interface AppkeyCallBack {

		public void releaseSdk();

		public int initSdk(String appkey);

		public void successUpdateUi(int code);

		public void failedUpdateUi(int code);

	}

}
