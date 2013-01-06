package de.hsbremen.android.convolution.nio;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import de.hsbremen.android.convolution.ProgressListener;
import de.hsbremen.android.convolution.buffer.NativeBuffers;

public class Processor
extends de.hsbremen.android.convolution.Processor {
	
	static {
		System.loadLibrary("convolute");
	}
	private IntBuffer  _kernel = null;
	public native void nativeProcess( ByteBuffer frame, int width, int height, byte bytesPerPixel, IntBuffer kernel, ByteBuffer output, ProgressListener progress );
	
	//private static native void nativeConvolute( ByteBuffer frame, int width, int height, IntBuffer kernel );

	// frame and kernel need to be directly allocated, so they can be easily accessed by
	// native code
	@Override
	public void process( ByteBuffer frame, int width, int height,
	                     int[] kernel,
	                     ProgressListener progress ) {
		if( _kernel == null || _kernel.capacity() < kernel.length )
			_kernel = NativeBuffers.allocateInt( kernel.length );
		
		_kernel.rewind();
		_kernel.put( kernel );
		nativeProcess( frame, width, height, BYTES_PER_PIXEL, _kernel, getOutputBuffer(), progress);
	}
}
