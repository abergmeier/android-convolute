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
import java.nio.ByteOrder;
import java.security.InvalidParameterException;
import java.util.Arrays;


//*
// @author Andreas Bergmeier
// Class for turning an ordinary byte stream containing RGB888
// data into a byte stream containing a BMP (Windows 3.0 format).
// Implementation based upon ij.plugin.BMP_Writer.
//*
public class BMPRGB888Stream
extends HeaderStream {
	private static final byte[] BMP_MAGIC = { 0x42, 0x4D };
	private static final int BITMAPFILEHEADER_SIZE = 14,
	                         BITMAPINFOHEADER_SIZE = 40,
	                         bfOffBits = BITMAPFILEHEADER_SIZE + BITMAPINFOHEADER_SIZE,
	                         biSize = BITMAPINFOHEADER_SIZE,
	                         biCompression   = 0,
	                         biXPelsPerMeter = 0x0,
	                         biYPelsPerMeter = 0x0,
	                         biClrUsed       = 0,
	                         biClrImportant  = 0;
	
	private static final short bfReserved1 = 0,
	                           bfReserved2 = 0,
	                           biPlanes    = 1,
	                           biBitCount  = 24;
	
	//*
	// By default wraps the file stream from top to bottom. To use native BMP format (bottom to top)
	// submit a negated height value.
	// @param rgbStream An InputStream, containing RGB888 data
	// @param width Width of RGB888 image. Throws InvalidParameterException when width is negative.
	// @param height Height of RGB888 image.
	//*
	public BMPRGB888Stream( InputStream rgbStream, int width, int height ) {
		// We have to add minus to height, because by default BMPs are saved from bottom to top
		super( createPaddedStream( rgbStream, width ), createHeader( width, -height ) );
	}
	
	private static ByteBuffer createHeader( final int width, final int height ) {
		final int rowSize = (width * biBitCount + 31) / 32 * 4,
		          pixelArraySize = rowSize * Math.abs( height ),
		          fileSize = biSize + pixelArraySize;
		
		final byte[] header = { 0, 0,       // Magic
		                        0, 0, 0, 0, // File Size
		                        0, 0,       // Reserved
		                        0, 0,       // Reserved
		                        0, 0, 0, 0, // Offset to pixel
		                        //INFO HEADER
		                        0, 0, 0, 0, // Size of header
		                        0, 0, 0, 0, // Width
		                        0, 0, 0, 0, // Height
		                        0, 0,       // Color plane count
		                        0, 0,       // Bits per Pixel
		                        0, 0, 0, 0, // Compression
		                        0, 0, 0, 0, // Pixel size
		                        0, 0, 0, 0, // x resolution
		                        0, 0, 0, 0, // y resolution
		                        0, 0, 0, 0, // Used color count
		                        0, 0, 0, 0, // Important color count
		};
			
		final ByteBuffer buffer = ByteBuffer.wrap( header ).order( ByteOrder.LITTLE_ENDIAN );
		buffer.put( BMP_MAGIC );
		intToDWord( buffer, fileSize );
		intToWord ( buffer, bfReserved1 );
		intToWord ( buffer, bfReserved2 );
		intToDWord( buffer, bfOffBits );
		// IJ.write("biClrUsed = " + biClrUsed + " bfSize = " + bfSize + " bfOffBits=" + bfOffBits);

		intToDWord( buffer, biSize          );
		intToDWord( buffer, width           );
		intToDWord( buffer, height          );
		intToWord ( buffer, biPlanes        );
		intToWord ( buffer, biBitCount      );
		intToDWord( buffer, biCompression   );
		intToDWord( buffer, pixelArraySize  );
		intToDWord( buffer, biXPelsPerMeter );
		intToDWord( buffer, biYPelsPerMeter );
		intToDWord( buffer, biClrUsed       );
		intToDWord( buffer, biClrImportant  );
		buffer.rewind();
		return buffer;
	}
		
	// intToWord converts an int to a word, where the return
	// value is stored in a 2-byte array.
	private static void intToWord( ByteBuffer buffer, short parValue ) {
		buffer.putShort( parValue );
	}

	 /*
	  *
	  * intToDWord converts an int to a double word, where the return
	  * value is stored in a 4-byte array.
	  *
	  */
	private static void intToDWord ( ByteBuffer buffer, int parValue) {
		buffer.putInt( parValue );
	}
	
	private static int getPad( int biWidth ) {
		int pad = 4 - ((biWidth * 3) % 4);
		if (pad == 4)       // <==== Bug correction
			pad = 0;            // <==== Bug correction
		return pad;
	}
		
	private static InputStream createPaddedStream( InputStream in, final int width ) {
		if( width < 0 )
			throw new InvalidParameterException( "Bitmap width has to be positive");
		
		final int pad = getPad( width );
		
		if( pad <= 0 )
			return in;
		
		final ByteBuffer padding = ByteBuffer.wrap( new byte[pad] );
		Arrays.fill( padding.array(), (byte)0x00 );
		padding.rewind();

		final InputStream stream = new FilterInputStream(in) {
			private int _xIndex = 0;
			@Override
			public int read() throws IOException {
				if( _xIndex == width ) {
					if( padding.hasRemaining() )
						return padding.get();
					else {
						padding.rewind();
						_xIndex = 0;
						return read();
					}
				}
				
				int result = super.read();
				++_xIndex;
				return result;
			}
			
			@Override
			public int read( byte[] buffer, final int origOffset, final int origCount )
			throws IOException {
				int count = origCount;
				if( count == 0 )
					return 0;
				
				int offset = origOffset;
				
				if( _xIndex == width ) {
					// Read padding
					if( padding.hasRemaining() ) {
						final int readCount = Math.min( padding.remaining(), count );
						padding.get( buffer, offset, readCount );
						offset += readCount;
						count -= readCount;
					} else {
						padding.rewind();
						_xIndex = 0;
					}
				} else {
					// Read ordinary x
					int readCount = Math.min( width - _xIndex,  count );
					readCount = super.read( buffer, offset, readCount );
					
					if( readCount == -1 )
						return -1;
					_xIndex += readCount;
					offset += readCount;
					count -= readCount;
				}
				
				// Recurse, should something be left to be done
				return (origOffset - offset) + read( buffer, offset, count );
			}
			
			@Override
			public long skip( final long origByteCount )
			throws IOException {
				long byteCount = origByteCount;
				if( byteCount == 0 )
					return 0;
				
				int leftX = width - _xIndex;
				leftX = Math.min( leftX, (int)byteCount );
				
				{ // Read ordinary x
					final int readCount = (int)super.skip( leftX );
					
					if( readCount == -1 )
						return -1;
					_xIndex += readCount;
					byteCount -= readCount;
				}
				
				{
					final int skipCount = Math.min( padding.remaining(), (int)byteCount);
					padding.position( padding.position() + skipCount );
					byteCount -= skipCount;
				}
				
				// Recurse, should something be left to be skipped
				return (origByteCount - byteCount) + skip( byteCount );
			}
		};
		return stream;
	}
}
