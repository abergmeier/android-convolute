package de.hsbremen.android.convolution.nio;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import de.hsbremen.android.convolution.IProcessor;
import de.hsbremen.android.convolution.ProgressListener;
import de.hsbremen.android.convolution.RenderListener;

public class Processor
implements IProcessor {
	
	static {
		System.loadLibrary("convolute");
	}
	
	public native void native_process( ByteBuffer frame, int width, int height, IntBuffer kernel, ProgressListener progress, RenderListener renderListener );
	
	//private static native void nativeConvolute( ByteBuffer frame, int width, int height, IntBuffer kernel );

	// frame and kernel need to be directly allocated, so they can be easily accessed by
	// native code
	@Override
	public void process( ByteBuffer frame, int width, int height, int[] kernel, ProgressListener progress, RenderListener renderListener ) {
		
	}
}
