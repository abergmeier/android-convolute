package de.hsbremen.android.convolution;

import java.io.IOException;

import android.app.ActionBar;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.TextView;

import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingActivity;

public class MainActivity
extends SlidingActivity {
	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	// Make sure we pin the buffer as long as necessary 
	private CameraProcessor _cameraProcessor = null;
	
	private static void LOGI( String msg ) {
		Log.i( LOG_TAG, msg );
	}
	
	private static void LOGV( String msg ) {
		Log.v(LOG_TAG, msg);
	}
	
	de.hsbremen.android.convolution.Fragment findFragment( int id ) {
		return (de.hsbremen.android.convolution.Fragment)getFragmentManager().findFragmentById( id );
	}
	
	private CameraProcessor.Listener findListener( int id ) {
		return findFragment( id ).getListener();
	}
	
	private void setName( int id, int stringId ) {
		de.hsbremen.android.convolution.Fragment fragment = findFragment( id );
		fragment.setName( stringId );
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView( R.layout.main_activity );
		setBehindContentView( R.layout.menu );
		
		{
			TextView textView = (TextView)findViewById( R.id.text_view );
			textView.setText( R.string.convolution_camera_name );
		}
		setName( R.id.convolution_java  , R.string.convolution_fragment_name_java   );
		setName( R.id.convolution_native, R.string.convolution_fragment_name_native );
		
		{
			SlidingMenu sm = getSlidingMenu();
			sm.setBehindWidthRes( R.dimen.back_width );
			sm.setTouchModeAbove( SlidingMenu.TOUCHMODE_FULLSCREEN );
		}

		{
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case android.R.id.home:
				toggle();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onPause() {
		LOGI("OnPause");
		super.onPause();
	}

	@Override
	protected void onStart() {
		super.onStart();

		TextureView textureView = (TextureView)findViewById( R.id.texture_view );
		textureView.setSurfaceTextureListener( new SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable( SurfaceTexture surface, int width, int height ) {
				
				if( _cameraProcessor != null ) {
					// Paranoia - should never happen!
					Log.e( getClass().getSimpleName(),
					       "Unexpected _cameraProcessor instance present - replacing!" );
				}
				
				try {
					_cameraProcessor = new CameraProcessor( surface,
					                                        findListener( R.id.convolution_java   ),
					                                        findListener( R.id.convolution_native ) );
				} catch (IOException e) {
					throw new RuntimeException( e );
				}
			}
			
			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				stopCamera();
				return false;
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
					int width, int height) {
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			}
		});
	}
	
	private void stopCamera() {
		if( _cameraProcessor != null ) {
			_cameraProcessor.close();
			_cameraProcessor = null;
		}
	}

	@Override
	protected void onStop() {
		stopCamera();
		
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
}
