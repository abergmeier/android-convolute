package de.hsbremen.android.convolution;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

public class CameraProcessor
implements PreviewCallback {
	public interface Listener {
		void onKernelChange( int[] kernel );
		// Copy the buffer into internal representation
		// After call ends, data is no longer valid
		void onPreviewFrameCopy( byte[] data );
		// Process last bound representation
		void onPreviewFrameProcess( int width, int height );
	}
	
	// Camera is not final because we need to close it.
	private       Camera _camera;
	private       ByteBuffer _cameraBuffer = null;
	private final Set<Listener> _listeners = new HashSet<Listener>();
	
	public CameraProcessor( SurfaceTexture surfaceTexture, Listener... listeners )
	throws IOException {
		_camera = Camera.open();
		allocateBuffer();
		
		for( Listener listener : listeners ) {
			_listeners.add( listener );
		}
		_camera.addCallbackBuffer( _cameraBuffer.array() );
		_camera.setPreviewCallbackWithBuffer( this );
		_camera.setPreviewTexture( surfaceTexture );
		_camera.startPreview();
	}

	// From http://developer.android.com/reference/android/hardware/Camera.Parameters.html#setPreviewFormat(int)
	private static int getYUVByteCount( int imageFormat, int width, int height ) {
		final int yStride   = (int)Math.ceil(width / 16.0) * 16;
		final int uvStride  = (int)Math.ceil( (yStride / 2) / 16.0) * 16;
		final int ySize     = yStride * height;
		final int uvSize    = uvStride * height / 2;
		return                ySize + uvSize * 2;
	}
/*
	private static int getRGBByteCount( int imageFormat, int width, int height ) {
		final int bytes = ImageFormat.getBitsPerPixel( imageFormat ) / 8;
		return            width * height * bytes;
	}
*/
	private void allocateBuffer() {
		Parameters parameters = _camera.getParameters();
		final Size size = parameters.getPreviewSize();
		final int previewFormat = parameters.getPreviewFormat();
		final int newByteCount = getYUVByteCount( previewFormat, size.width, size.height );
		
		_cameraBuffer = ByteBuffer.allocate( newByteCount );
	}
	
	@Override
	public synchronized void onPreviewFrame( byte[] data, Camera camera ) {
		// Paranoia option. Make sure our later assumption is really valid.
		for( Listener listener : _listeners ) {
			listener.onPreviewFrameCopy( data );
		}
		
		// Data was copied so we make the buffer available again 
		_camera.addCallbackBuffer( data );

		final Size previewSize = _camera.getParameters().getPreviewSize();
		for( Listener listener : _listeners ) {
			listener.onPreviewFrameProcess( previewSize.width, previewSize.height );
		}
	}
	
	public void close() {
		_camera.stopPreview();
		_camera.setPreviewCallbackWithBuffer( null );
		_listeners.clear();
		_camera.release();
		_camera = null;
	}
}
