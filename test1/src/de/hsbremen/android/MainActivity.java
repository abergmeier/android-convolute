package de.hsbremen.android;

import com.example.test1.R;
import com.example.test1.R.layout;
import com.example.test1.R.menu;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

public class MainActivity
extends Activity {
	
	private static final String LOG_TAG = "HSBREMEN";
	
	private static void LOGI( String msg ) {
		Log.i( LOG_TAG, msg );
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		LOGI("OnCreate");
		super.onCreate(savedInstanceState);
		setContentView( R.layout.activity_main );
	}
	
	@Override
	protected void onPause() {
		LOGI("OnPause");
		super.onPause();
	}

	@Override
	protected void onStart() {
		LOGI("OnStart");
		super.onStart();
	}

	@Override
	protected void onStop() {
		LOGI("OnStop");
		super.onStop();
	}
	
	@Override
	protected void onRestart() {
		LOGI("OnRestart");
		super.onRestart();
	}
	
	@Override
	protected void onResume() {
		LOGI("OnResume");
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		LOGI("OnDestroy");
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}
