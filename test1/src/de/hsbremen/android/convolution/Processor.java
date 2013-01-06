package de.hsbremen.android.convolution;

import java.nio.ByteBuffer;

import android.util.Log;

public abstract class Processor {
	public static final byte BYTES_PER_PIXEL = 3;
	protected static void LOGV( String message ) {
		Log.v( Processor.class.getSimpleName(), message );
	}
	
	private ByteBuffer _output = null;
	
	public final void convolute( ByteBuffer frame,
	                             int width, int height,
	                             int[] kernel,
	                             ProgressListener progress, RenderListener renderListener ) {
		reserveOutputBuffer( width, height );
		progress.reset();
		process( frame, width, height, kernel, progress );
		_output.rewind();
/*
		GLES20Texture.glTexImage2D( GLES20.GL_RGB, width / 4, height / 4, GLES20.GL_UNSIGNED_BYTE, _output );
		GLUtils.throwOnError( "glEGLImageTargetTexture2DOES" );
*/
		renderListener.onRendered( _output, width, height );
	}
	
	protected abstract void process( ByteBuffer frame,
	                                 int width, int height,
	                                 int[] kernel,
	                                 ProgressListener progress );
	
	protected void reserveOutputBuffer( int width, int height ) {
		final int pixels = width * height;
		final int size = pixels * BYTES_PER_PIXEL;
		
		if( _output == null || _output.capacity() < size ) {
			// We have to allocate buffer native, so we can pass it
			// to gl routines
			_output = NativeBuffers.allocateByte( size );
			LOGV( "Reallocated output buffer with size: " + size );
		}
		
		_output.limit( size );
	}
	
	protected final ByteBuffer getOutputBuffer() {
		return _output;
	}
}
