package de.hsbremen.android.convolution.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import android.util.Log;

public class BMPRGB888Stream
extends HeaderStream {
	private static final byte[] BMP_MAGIC = { 0x42, 0x4D };
	private static final int BITMAPFILEHEADER_SIZE = 14,
	                         BITMAPINFOHEADER_SIZE = 40,
	                         bfOffBits = BITMAPFILEHEADER_SIZE + BITMAPINFOHEADER_SIZE,
	                         biSize = BITMAPINFOHEADER_SIZE,
	                         biCompression   = 0,
	                         biSizeImage     = 0,
	                         biXPelsPerMeter = 0x0,
	                         biYPelsPerMeter = 0x0,
	                         biClrUsed       = 0,
	                         biClrImportant  = 0;
	
	private static final short bfReserved1 = 0,
	                           bfReserved2 = 0,
	                           biPlanes    = 1,
	                           biBitCount  = 24;
	
	public BMPRGB888Stream( InputStream rgbStream, int width, int height ) {
		super( createPaddedStream( rgbStream, width ), createHeader( width, height ) );
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
	/*
		buffer.put( (byte) ((parValue >> 0) & 0x00FF) );
		buffer.put( (byte) ((parValue >> 8) & 0x00FF) );
	 */
	}

	 /*
	  *
	  * intToDWord converts an int to a double word, where the return
	  * value is stored in a 4-byte array.
	  *
	  */
	private static void intToDWord ( ByteBuffer buffer, int parValue) {
		buffer.putInt( parValue );
/*
		   byte retValue [] = new byte [4];
		   retValue [0] = (byte) (parValue & 0x00FF);
		   retValue [1] = (byte) ((parValue >> 8) & 0x000000FF);
		   retValue [2] = (byte) ((parValue >> 16) & 0x000000FF);
		   retValue [3] = (byte) ((parValue >> 24) & 0x000000FF);
		   return (retValue);
*/
	}
	
	private static int getPad( int biWidth ) {
		int pad = 4 - ((biWidth * 3) % 4);
		if (pad == 4)       // <==== Bug correction
			pad = 0;            // <==== Bug correction
		return pad;
	}
		
	private static InputStream createPaddedStream( InputStream in, final int width ) {
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
			
			private void LOGV( String message ) {
				Log.v( getClass().getSimpleName(), message );
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
						LOGV( "Read from pad: " + readCount + " of " + origCount );
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
	 
/*
	private void writeBitmap ( ByteBuffer buffer ) {
		
		for( int row = biHeight; row > 0; row-- ) {
			for(int col = 0; col<biWidth; col++) {
				final int value = intBitmap [(row-1) * biWidth + col ];
				
				buffer.put( (byte)((value >>  0) & 0xFF) );
				buffer.put( (byte)((value >>  8) & 0xFF) );
				buffer.put( (byte)((value >> 16) & 0xFF) );
			}
			
		}
	}
*/
}