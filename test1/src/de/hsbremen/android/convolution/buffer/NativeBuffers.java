package de.hsbremen.android.convolution.buffer;

/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 * @author Andreas Bergmeier
 * Utility class for allocating direct (native) Buffers using
 * native ByteOrder
 */
public abstract class NativeBuffers {
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
