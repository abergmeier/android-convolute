package de.hsbremen.android.convolution.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.util.Log;

public class HeaderStream
extends FilterInputStream {
	private ByteBuffer _header;
	
	protected HeaderStream( InputStream stream, ByteBuffer header ) {
		super( stream );
		_header = header;
	}
	
	private ByteBuffer header() {
		return _header;
	}
	
	@Override
	public int read() throws IOException {
		if( header().hasRemaining() )
			return header().get();
		else
			return super.read();
	}
	
	private void LOGV( String message ) {
		Log.v( getClass().getSimpleName(), message );
	}
	
	@Override
	public int read( byte[] buffer, int offset, final int origCount ) throws IOException {
		int count = origCount;
		if( header().hasRemaining() ) {
			// Read header first
			int readHeaderCount = Math.min( header().remaining(), count );
			header().get( buffer, offset, readHeaderCount );
			count -= readHeaderCount;
			offset += readHeaderCount;
			LOGV( "Read from hed: " + readHeaderCount + " of " + origCount + " " + super.getClass().getSimpleName());
		}
		
		return (origCount - count) + super.read( buffer, offset , count );
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		_header = null;
	}
	
	@Override
	public int available() throws IOException {
		return header().remaining() + super.available();
	}
	
	@Override
	public long skip(final long origByteCount) throws IOException {
		long byteCount = origByteCount;
		if( byteCount < 0 )
			return super.skip(byteCount);
		
		if( header().hasRemaining() ) {
			final int delta = Math.min( (int)byteCount, header().remaining() );
			header().position( header().position() + delta );
			byteCount -= delta;
		}
		
		return ( origByteCount - byteCount ) + super.skip(byteCount);
	}
}
