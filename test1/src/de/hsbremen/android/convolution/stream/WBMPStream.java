package de.hsbremen.android.convolution.stream;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;


public class WBMPStream
extends HeaderStream {
	public WBMPStream( InputStream inputStream, int width, int height ) {
		super( inputStream, createHeader(width, height) );
	}
	
	private static ByteBuffer createHeader( int width, int height ) {
		final byte[] header = new byte[] { 0,          //Type
		                                   0,          //Fixed header
		                                   0, 0, 0, 0, //Width
		                                   0, 0, 0, 0  //Data
		                                 };
		encode( header, 2, width  );
		encode( header, 6, height );
		return ByteBuffer.wrap( header );
	}
	
	private static void encode( byte[] array, int offset, int value ) {
		if( (value & 0xF0000000) != 0 )
			throw new InvalidParameterException("Value " + value + "is too big to be encoded (max: " + 0x0FFFFFFF + ")");

		// | 0x80 makes sure, upper most bit is set so next byte is processed
		// & 0x7F makes sure, upper most bit is NOT set to end processing
		array[offset + 0] = (byte)((value >> 21) | 0x80);
		array[offset + 1] = (byte)((value >> 14) | 0x80);
		array[offset + 2] = (byte)((value >>  7) | 0x80);
		array[offset + 3] = (byte)((value >>  0) & 0x7F);
	}
}
