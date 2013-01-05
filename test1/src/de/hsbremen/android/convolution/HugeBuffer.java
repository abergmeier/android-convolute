package de.hsbremen.android.convolution;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

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
	
	class NativeVariant {
		public final ByteBuffer buffer;
		
		public NativeVariant( int byteLength ) {
			buffer = NativeBuffers.allocateByte(byteLength);
		}

		public void close() {
		}
	}
	
	private NativeVariant    _native;
	private FileVariant      _file;
	private final ByteBuffer _buffer;
	
	public HugeBuffer( File cacheDir, int byteLength ) {
		try {
			//_native = new NativeVariant( byteLength );
			_file = null;
		} catch( OutOfMemoryError memory ) {
		}
		
		if( _native == null ) {
			try {
				_file = new FileVariant( cacheDir, byteLength );
			} catch( IOException e ) {
				throw new RuntimeException( e );
			}
		}
		
		if( _file == null )
			_buffer = _native.buffer;
		else
			_buffer = _file.buffer;
	}

	public int capacity() {
		return _buffer.capacity();
	}

	public void rewind() {
		_buffer.rewind();		
	}

	public void put( byte[] data ) {
		_buffer.put( data );		
	}

	public int get(int i) {
		return _buffer.get(i);
	}

	public void close() {
		if( _native == null )
			_file.close();
		else
			_native.close();
		
	}
}