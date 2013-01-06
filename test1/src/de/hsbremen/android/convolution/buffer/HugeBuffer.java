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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel.MapMode;


/**
 * Buffer implementation, which wraps a ByteBuffer, which is itself backed by a temporary File.
 * Content is kept in memory as long as there is enough space, and written to the File if not.
 * Work in progress!
 * @author Andreas Bergmeier
 */
public class HugeBuffer {
	class FileVariant {
		public final File tempFile;
		public final RandomAccessFile file;
		public final MappedByteBuffer buffer;
		
		public FileVariant( File cacheDir, int byteLength )
		throws IOException {
			tempFile = File.createTempFile( "temp", "buffer", cacheDir );
			file = new RandomAccessFile( tempFile, "rw" );
			buffer = file.getChannel().map( MapMode.READ_WRITE, 0, byteLength );
		}
		
		public void close() {
			try {
				file.close();
			} catch (IOException e) {
			}
			tempFile.delete();
		}
	}
	
	private FileVariant      _file;
	private final ByteBuffer _buffer;
	
	public HugeBuffer( File cacheDir, int byteLength ) {
		try {
			_file = new FileVariant( cacheDir, byteLength );
		} catch( IOException e ) {
			throw new RuntimeException( e );
		}
		
		_buffer = _file.buffer;
	}
	
	private HugeBuffer( FileVariant file, ByteBuffer buffer ) {
		_file = file;
		_buffer = buffer;
	}
	
	public ByteBuffer asByteBuffer() {
		return _buffer;
	}
	
	public CharBuffer asCharBuffer() {
		return _buffer.asCharBuffer();
	}
	
	public DoubleBuffer asDoubleBuffer() {
		return _buffer.asDoubleBuffer();
	}
	
	public FloatBuffer asFloatBuffer() {
		return _buffer.asFloatBuffer();
	}
	
	public IntBuffer asIntBuffer() {
		return _buffer.asIntBuffer();
	}
	
	public LongBuffer asLongBuffer() {
		return _buffer.asLongBuffer();
	}
	
	public ByteBuffer asReadOnlyBuffer() {
		return _buffer.asReadOnlyBuffer();
	}
	
	public ShortBuffer asShortBuffer() {
		return _buffer.asShortBuffer();
	}
	
	public int capacity() {
		return _buffer.capacity();
	}
	
	public void close() {
		_file.close();
		_file = null;
	}
	
	public ByteBuffer compact() {
		return _buffer.compact();
	}
	
	public int compareTo(ByteBuffer that) {
		return _buffer.compareTo(that);
	}
	
	public HugeBuffer duplicate() {
		return new HugeBuffer( _file, _buffer );
	}
	
	public boolean equals(Object ob) {
		return _buffer.equals(ob);
	}
	
	public byte get() {
		return _buffer.get();
	}
	
	public ByteBuffer get(byte[] dst) {
		return _buffer.get(dst);
	}
	
	public ByteBuffer get( byte[] dst, int offset, int length ) {
		return _buffer.get(dst, offset, length);
	}
	
	public byte get(int index) {
		return _buffer.get(index);
	}
	
	public void put( byte[] data ) {
		_buffer.put( data );		
	}

	public HugeBuffer rewind() {
		_buffer.rewind();
		return this;
	}
}