package de.hsbremen.android.convolution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import de.hsbremen.android.convolution.stream.BMPRGB888Stream;
import de.hsbremen.android.convolution.stream.ByteBufferInputStream;

public abstract class Fragment
extends android.app.Fragment {
	private   Renderer<?>              _renderer = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu( true );
	}
	
	protected abstract Renderer<?> createRenderer( ProgressListener progress, RenderListener render );
	
	@Override
	public void onStart() {
		super.onStart();
		
		final View view = (View)getView();
		
		final RenderListener   renderListener   = new RenderListener() {
			private final ImageView             _imageView = (ImageView)view.findViewById( R.id.image_view );
			private final ByteBufferInputStream _stream = new ByteBufferInputStream();
			private       Bitmap _bitmap = null;
			
			@Override
			public void onRendered( ByteBuffer output, int width, int height ) {
				final Options options = new Options();
				// Calculate only necessary pixels
				options.inSampleSize = calculateSampleSize( width, height, _imageView.getWidth(), _imageView.getHeight() );
				
				final Bitmap bitmap;
				{
					InputStream stream = new BMPRGB888Stream( _stream.set( output ), width, height );
					try {
						bitmap = BitmapFactory.decodeStream( stream, null, options );
					} finally {
						try {
							stream.close();
						} catch (IOException e) {
						}
					}
				}

				if( bitmap == null ) {
					Log.e( getClass().getSimpleName(), "Processed Image could not be decoded" );
					return;
				}

				// Changing bitmap HAS to be execute in UI thread!
				_imageView.post( new Runnable() {
					@Override
					public void run() {
						_imageView.setImageBitmap( bitmap );
					}
				} );
			}
			
			// See: http://stackoverflow.com/questions/1322510/given-an-integer-how-do-i-find-the-next-largest-power-of-two-using-bit-twiddlin
			private int next2Pow( int n ) {
				n--;
				n |= n >> 1;
				n |= n >> 2;
				n |= n >> 4;
				n |= n >> 8;
				n |= n >> 16;
				n++;
				return n;
			}
			
			
			// See: http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
			private int calculateSampleSize( int width, int height, int reqWidth, int reqHeight ) {
				int inSampleSize = 1;

				if( height > reqHeight || width > reqWidth ) {
					if (width > height) {
						inSampleSize = Math.round((float)height / (float)reqHeight);
					} else {
						inSampleSize = Math.round((float)width / (float)reqWidth);
					}
				}
				return inSampleSize;
			}
		};
		final ProgressListener progressListener = new ProgressDelegator( (ProgressBar)view.findViewById( R.id.progress_bar ) );
		
		_renderer = createRenderer( progressListener, renderListener );
		_renderer.listener.onKernelChange( Kernel.NEUTRAL.array );
		
/*
		{
			TextureView textureView = (TextureView)getView();
			textureView.setSurfaceTextureListener( _listener );
		}
*/
	}
	
	public CameraProcessor.Listener getListener() {
		return _renderer.listener;
	}
	
	@Override
	public void onStop() {
		super.onStop();
/*
		{
			TextureView textureView = (TextureView)getView();
			textureView.setSurfaceTextureListener( null );
		}
*/
		
		_renderer.close();
		_renderer = null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate( R.menu.convolute_menu, menu );
	}
	
	private static enum Kernel {
		NEUTRAL(
			0, 0, 0,
			0, 1, 0,
			0, 0, 0
		),
		BLUR(
			1, 1, 1,
			1, 1, 1,
			1, 1, 1
		),
		EDGE(
			0,  1, 0,
			1, -4, 1,
			0,  1, 0
		);
		
		private final int[] array;
		
		private Kernel(int... values) {
			array = values;
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch( item.getItemId() ) {
		case R.id.convolute_blur:
			getListener().onKernelChange( Kernel.BLUR.array );
			return true;
		case R.id.convolute_edge:
			getListener().onKernelChange( Kernel.EDGE.array );
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private boolean ensureOpenGLES20() 
	{
		ActivityManager am = (ActivityManager)getActivity().getSystemService( Context.ACTIVITY_SERVICE );
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		return info.reqGlEsVersion >= 0x20000
		    || Build.FINGERPRINT.startsWith("generic"); // Fallback for emulator
	}
	/*
	private static final int[] EGL_ATTRIBUTES = new int[] {
			EGL10.EGL_NONE
	};
	
	private EGLConfig[] getConfig( EGL10 egl, EGLDisplay display ) {
		EGLConfig[] configs = new EGLConfig[128];
		int[] configCount = new int[] {0};
		boolean success = egl.eglChooseConfig( display, EGL_ATTRIBUTES, configs, configs.length, configCount );
		if( !success )
			throw new RuntimeException( "eglChooseConfig failed" );
		return configs;
	}
	*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if( !ensureOpenGLES20() ) {
			throw new RuntimeException("OpenGL ES 2.0 needed but not supported.");
		}
		
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.gl_view, container, false);
	}
}
