package de.hsbremen.android.convolution.stream;

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
