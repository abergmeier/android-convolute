package de.hsbremen.android.convolution.java;

import java.nio.ByteBuffer;

import de.hsbremen.android.convolution.ProgressListener;
import de.hsbremen.android.convolution.buffer.NativeBuffers;

public class Processor
extends de.hsbremen.android.convolution.Processor {
	private ByteBuffer _input  = null;
	
	public Processor() {
	}

	private static int getByteOffset( int x, int y, int width ) {
		return (x + y * width) * BYTES_PER_PIXEL;
	}

	private void setPixel( ByteBuffer pixels, byte r, byte g, byte b ) {
		pixels.put( r );
		pixels.put( g );
		pixels.put( b );
	}

	private static int clipToByte( int color ) {
		return Math.max(Math.min(color, 255), 0);
	}
	
	private byte getComponent( int byteOffset, int componentOffset ) {
		return _input.get( byteOffset + componentOffset );
	}
	
	private byte red  ( int byteOffset ) { return getComponent( byteOffset, 0 ); }
	private byte green( int byteOffset ) { return getComponent( byteOffset, 1 ); }
	private byte blue ( int byteOffset ) { return getComponent( byteOffset, 2 ); }

	public static void convertYUV420_NV21toRGB888(ByteBuffer data, int width, int height, ByteBuffer dest, ProgressListener progress) {
		final int size = width * height;
		final int offset = size;

		//i percorre os Y and the final pixels
		// k percorre os pixles U e V
		for( int i = 0, k = 0; i < size; i += 2, k += 2) {
			{
				final byte y1 = data.get(i  );
				final byte y2 = data.get(i+1);
				final byte y3 = data.get(width+i  );
				final byte y4 = data.get(width+i+1);
	
				int u = data.get(offset+k  ) & 0xFF;
				int v = data.get(offset+k+1) & 0xFF;
				u = u - 128;
				v = v - 128;
	
				convertYUVtoRGB( y1, u, v, dest, i           );
				convertYUVtoRGB( y2, u, v, dest, i + 1       );
				convertYUVtoRGB( y3, u, v, dest, width + i   );
				convertYUVtoRGB( y4, u, v, dest, width+ i +1 );
			}

			if( i != 0 && ( i + 2 ) % width == 0 ) {
				i += width;
				progress.incrementBy( width * 2 );
			}
		}
	}

	private static int toInt( byte b ) {
		return b & 0xFF;
	}
	
	private static byte toByte( int value ) {
		return (byte)Math.max( Math.min(value, 0xFF), 0 );
	}
	
	private static void convertYUVtoRGB( byte y, int u, int v, ByteBuffer dest, int rgbIndex ) {
		final int r = toInt(y) + (int)(1.402f * v);
		final int g = toInt(y) - (int)(0.344f * u + 0.714f * v);
		final int b = toInt(y) + (int)(1.772f * u);

		final int index = rgbIndex * BYTES_PER_PIXEL;
		dest.put(index + 0, toByte(r) );
		dest.put(index + 1, toByte(g) );
		dest.put(index + 2, toByte(b) );
	}

	@Override
	public void process( ByteBuffer frame, int width, int height, int[] kernel, ProgressListener progress ) {
		final int pixels = width * height;
		final int size = pixels * BYTES_PER_PIXEL;
		
		if( _input == null || _input.capacity() < size ) {
			_input = NativeBuffers.allocateByte( size );
			LOGV("Reallocated convert buffer with size: " + size );
		}
		
		// We process image twice so display twice
		progress.setMax( pixels * 2 );
		
		//convertRGB565to888( frame, _input );
		_input.rewind();
		convertYUV420_NV21toRGB888( frame, width, height, _input, progress );
		
		_input.rewind();
		getOutputBuffer().rewind();
		getOutputBuffer().put( _input );
		//convolute( width, height, kernel, progress );
	}
		
	private void convolute( int width, int height, int[] kernel, ProgressListener progress ) {
		
		for( int y = 0; y < height; ++y ) {
			for (int x = 0; x < width; ++x ) {
/*
					X X X
					X X X
					X X X
*/
				int r = 0,
				    g = 0,
				    b = 0;

				for( int dx = -1; dx != 2; ++dx ) {
					for( int dy = -1; dy != 2; ++dy ) {
						final int kern = kernel[(1 + dx) + (1 + dy) * 3];
						final int local_x = x + dx;
						final int local_y = y + dy;

						if( local_x < 0 || local_x >= width || local_y < 0 || local_y >= height )
							continue;
						
						final int byteOffset = getByteOffset( local_x, local_y, width );
						r += red  ( byteOffset ) * kern;
						g += green( byteOffset ) * kern;
						b += blue ( byteOffset ) * kern;
						r = clipToByte(r);
						g = clipToByte(g);
						b = clipToByte(b);
					}
				}

				setPixel( getOutputBuffer(), (byte)r, (byte)g, (byte)b );
				//setPixel( _output, x, y, width, (byte)r, (byte)g, (byte)b );
			}
			progress.incrementBy( width );
			
		}
	}

}
