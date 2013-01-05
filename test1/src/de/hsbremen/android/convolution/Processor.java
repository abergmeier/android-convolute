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
