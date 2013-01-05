package de.hsbremen.android.convolution;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public final class NativeBuffers {
	public static ByteBuffer allocateByte( int elementCount ) {
		return ByteBuffer.allocateDirect( elementCount )
		                 .order(ByteOrder.nativeOrder());
	}
	
	public static IntBuffer allocateInt( int elementCount ) {
		return allocateByte( elementCount * Integer.SIZE / Byte.SIZE ).asIntBuffer();
	}
	
	public static LongBuffer allocateLong( int elementCount ) {
		return allocateByte( elementCount * Long.SIZE / Byte.SIZE ).asLongBuffer();
	}

	public static FloatBuffer allocateFloat( int elementCount ) {
		return allocateByte( elementCount * Float.SIZE / Byte.SIZE ).asFloatBuffer();
	}
}
