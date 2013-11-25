package com.bmw.android.ais;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.LinearLayout;

import com.bmw.android.androidindexer.FileIndexer;
import com.bmw.android.indexservice.IndexService;

public class MainActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*new Thread(new Runnable() {
			@Override
			public void run() {
				if (!isMyServiceRunning()) {
					Intent serviceIntent = new Intent(MainActivity.this, IndexService.class);
					startService(serviceIntent);
					Log.e("AIS", "Started Service");
				} else {
					Log.e("AIS", "Service Already Running");
				}
			}
		}).start();*/
		if(!this.isMyServiceRunning()){
        	Intent serviceIntent = new Intent(this, IndexService.class);
			this.startService(serviceIntent);
        }
		this.setContentView(new LinearLayout(this));
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (IndexService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
