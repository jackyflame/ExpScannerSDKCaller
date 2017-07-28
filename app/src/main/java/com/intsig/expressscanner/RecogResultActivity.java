package com.intsig.expressscanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class RecogResultActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.ac_recog);
		Intent intent = getIntent();
		String result = intent
				.getStringExtra(PreviewActivity.EXTRA_KEY_RESULT_DATA);

	int type = intent
			.getIntExtra(PreviewActivity.EXTRA_KEY_RESULT_TYPE,0);
		TextView nameidTextView = (TextView) findViewById(R.id.tv_label_name);
		TextView valueidTextView = (TextView) findViewById(R.id.tv_label_name_value);
		String regtimeall = null;
		// 图片角度,如果有图片的话
				if (TextUtils.isEmpty(regtimeall)) {
					findViewById(R.id.use_time_row_id).setVisibility(View.GONE);
				} else {

					((TextView) findViewById(R.id.use_time_id)).setText(regtimeall
							+ "ms");
				}
		// 图片角度,如果有图片的话
		if (TextUtils.isEmpty(regtimeall)) {
			findViewById(R.id.use_time_row_id).setVisibility(View.GONE);
		} else {

			((TextView) findViewById(R.id.use_time_id)).setText(regtimeall
					+ "ms");
		}
		if (!TextUtils.isEmpty(result)) {
			if (type == 2) {
				nameidTextView.setText("一维码：");
				valueidTextView.setText(result);

			} else {
				nameidTextView.setText("手机号：");
				valueidTextView.setText(result);
			}

		}

	}

	public void onClick(View view) {
		int id = view.getId();

		
		
		if (!MainActivity.boolCheckAppKey) {

			Intent intent = new Intent(this, PreviewActivity.class);
			// 合合信息授权提供的APP_KEY
			intent.putExtra(PreviewActivity.EXTRA_KEY_APP_KEY,
					MainActivity.APP_KEY);
			startActivity(intent);
			finish();
		} else {
			finish();
		}
	}

}
