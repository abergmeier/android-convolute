package de.hsbremen.android.convolution.nio;

import android.graphics.SurfaceTexture;
import de.hsbremen.android.convolution.IProcessor;

public class Processor
implements IProcessor {
	
	static {
		System.loadLibrary("fib");
	}
	
	private static native void nativeConvolute( int width, int height, int texName, int[] kernel );

	public void Process(SurfaceTexture texture, int width, int height, int[] kernel) {
		nativeConvolute( texture.getTexName(), kernel );
	}

}
