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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.util.Log;

public class ByteBufferInputStream
extends InputStream {

	private ByteBuffer _buf;

	public ByteBufferInputStream() {
	}
	
	@Override
	public synchronized int read()
	throws IOException {
		if( !_buf.hasRemaining() )
			return -1;

		return _buf.get();
	}
	
	private void LOGV( String message ) {
		Log.v( getClass().getSimpleName(), message );
	}

	@Override
	public synchronized int read( byte[] bytes, int off, final int origLen )
	throws IOException {
		int remaining = _buf.remaining();
		if( remaining <= 0 )
			return -1;

		int len = origLen;
		len = Math.min( len, remaining );
		_buf.get( bytes, off, len );
		if( len != origLen )
			LOGV( "read from buf: " + len + " of " + origLen );
		return len;
	}
	
	@Override
	public long skip( long byteCount ) throws IOException {
		if( byteCount < 0 )
			return super.skip(byteCount);
		else if( byteCount == 0 )
			return 0;
		byteCount = Math.min( byteCount, _buf.remaining() );
		_buf.position( _buf.position() + (int)byteCount );
		return byteCount;
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		_buf = null;
	}
	
	@Override
	public int available() throws IOException {
		return _buf.remaining();
	}
	
	@Override
	public boolean markSupported() {
		return false;
	}

	public InputStream set( ByteBuffer buffer ) {
		_buf = buffer;
		return this;
	}
}
