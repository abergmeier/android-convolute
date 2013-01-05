package de.hsbremen.android.convolution.java;

import java.nio.ByteBuffer;

import android.util.Log;
import de.hsbremen.android.convolution.IProcessor;
import de.hsbremen.android.convolution.NativeBuffers;
import de.hsbremen.android.convolution.ProgressListener;
import de.hsbremen.android.convolution.RenderListener;

public class Processor
implements IProcessor {
	private ByteBuffer _input  = null;
	private ByteBuffer _output = null;
	
	public Processor() {
	}
/*
	private static int getOffset( int x, int y, int width ) {
		return x + y * width;
	}
*/
	private static int getByteOffset( int x, int y, int width ) {
		return x * 3 + y * width;
	}
/*
	private Integer getPixel( int x, int y, int width) {
		final int offset = getByteOffset( x, y, width );
		//return _input.array()[_input.arrayOffset() + offset];
		return _input[offset];
	}
*/
	private void setPixel( ByteBuffer pixels, int x, int y, int width, byte r, byte g, byte b ) {
		final int byteOffset = getByteOffset( x, y, width );
		
		if( pixels.hasArray() ) {
			byte[] buffer = pixels.array();
			
			buffer[pixels.arrayOffset() + byteOffset + 0] = r;
			buffer[pixels.arrayOffset() + byteOffset + 1] = g;
			buffer[pixels.arrayOffset() + byteOffset + 2] = b;
		} else {
			pixels.put(byteOffset + 0, r);
			pixels.put(byteOffset + 1, g);
			pixels.put(byteOffset + 1, b);
		}
	}
	
	void onDrawFrame() {
		
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
/*
	private int getKernel( IntBuffer buffer, int index ) {
		return buffer.get(index);
	}
	
	private static void convertNV21toRGB( ByteBuffer frame, int width, int height, ByteBuffer dest ) {
		final int size = width * height;
		final int offset = size;
	 
		// i along Y and the final pixels
		// k along pixels U and V
		for( int i = 0, k = 0; i < size; i += width ) {
			for( ; i == 0 || (i + 2) % width != 0; i += 2, k += 2 ) {
				final int y1 = frame.get(i + 0)         & 0xFF;
				final int y2 = frame.get(i + 1)         & 0xFF;
				final int y3 = frame.get(i + width + 0) & 0xFF;
				final int y4 = frame.get(i + width + 1) & 0xFF;
		
				int u = frame.get(offset + k + 0) & 0xFF;
				int v = frame.get(offset + k + 1) & 0xFF;
				u = u-128;
				v = v-128;
		
				convertYUVtoRGB( y1, u, v, dest, i + 0 ); Log.v("TEST", y1 + " " + u + " " + v);
				convertYUVtoRGB( y2, u, v, dest, i + 1 );
				convertYUVtoRGB( y3, u, v, dest, i + width + 0);
				convertYUVtoRGB( y4, u, v, dest, i + width + 1);
			}
		}
	}
	
	private static byte clampToByte( int value ) {
		return (byte)Math.max( Math.min( value, 0xFF), 0 );
	}
	
	private static void con( int y, int u, int v, ByteBuffer dest, int index ) {
		int r,g,b;

	    r = y + (int)1.402f*v;
	    g = y - (int)(0.344f*u +0.714f*v);
	    b = y + (int)1.772f*u;
	    r = r>255? 255 : r<0 ? 0 : r;
	    g = g>255? 255 : g<0 ? 0 : g;
	    b = b>255? 255 : b<0 ? 0 : b;
	    dest.put( index + 0, (byte)(b & 0xFF) );
	    dest.put( index + 1, (byte)(g & 0xFF) );
	    dest.put( index + 2, (byte)(r & 0xFF) );
	}
	
	private static void convertYUVtoRGB( int y, int u, int v, ByteBuffer dest, int index ) {
		int b = (int)(1.164 * ( y - 16)                   + 2.018 * (u));

		int g = (int)(1.164 * ( y - 16) - 0.813 * ( v ) - 0.391 * (u));

		int r = (int)(1.164 * ( y - 16 ) + 1.596 * ( v ));
				
		//final int r = y + (int)(1.402f * v             );
		//final int g = y - (int)(0.344f * u + 0.714f * v);
		//final int b = y + (int)(1.772f * u             );
		
		dest.put( index + 0, clampToByte(r) );
		dest.put( index + 1, clampToByte(g) );
		dest.put( index + 2, clampToByte(b) );
	}
*/
	public static void convertYUV420_NV21toRGB888(ByteBuffer data, int width, int height, ByteBuffer dest, ProgressListener progress) {
	    int size = width * height;
	    int offset = size;
	    int u, v, y1, y2, y3, y4;

	    // i percorre os Y and the final pixels
	    // k percorre os pixles U e V
	    for(int i=0, k=0; i < size; i+=2, k+=2) {
	        y1 = data.get(i  )&0xff;
	        y2 = data.get(i+1)&0xff;
	        y3 = data.get(width+i  )&0xff;
	        y4 = data.get(width+i+1)&0xff;

	        u = data.get(offset+k  )&0xff;
	        v = data.get(offset+k+1)&0xff;
	        u = u-128;
	        v = v-128;

	        convertYUVtoRGB(y1, u, v, dest, i );
	        convertYUVtoRGB(y2, u, v, dest, i + 1);
	        convertYUVtoRGB(y3, u, v, dest, width + i);
	        convertYUVtoRGB(y4, u, v, dest, width+ i +1);

	        if (i!=0 && (i+2)%width==0) {
	            i+=width;
	            progress.incrementBy( width );
	        } else
	            progress.incrementBy( 2 );
	    }
	    
	}

	private static void convertYUVtoRGB(int y, int u, int v, ByteBuffer dest, int rgbIndex) {
	    int r,g,b;
	    
	    r = y + (int)1.402f*v;
	    g = y - (int)(0.344f*u +0.714f*v);
	    b = y + (int)1.772f*u;
	    r = r>255? 255 : r<0 ? 0 : r;
	    g = g>255? 255 : g<0 ? 0 : g;
	    b = b>255? 255 : b<0 ? 0 : b;
	    final int index = rgbIndex * 3;
	    dest.put(index+0, (byte)r   );
	    dest.put(index+1, (byte)g   );
	    dest.put(index+2, (byte)b   );
	}
	
	private static void LOGV( String message ) {
		Log.v( Processor.class.getSimpleName(), message );
	}
	
	private static byte convert( ByteBuffer frame, float ratio ) {
		int newValue = (int)(frame.get() * ratio);
		newValue = Math.max( Math.min( newValue, 0xFF ), 0 );
		return (byte)newValue;
	}

	@Override
	public void process( ByteBuffer frame, int width, int height, int[] kernel, ProgressListener progress, RenderListener renderListener ) {
/*
		if( _input == null || _input.capacity() < frame.array().length )
			_input = NativeBuffers.allocateLong( width * height ); 
		
		if( _output == null || _output.capacity() < frame.array().length )
			_output = NativeBuffers.allocateLong( width * height );
*/
		progress.reset();
		final int pixels = width * height;
		final int size = pixels * 3;
		
		if( _input == null || _input.capacity() < size ) {
			_input = NativeBuffers.allocateByte( size );
			LOGV("Reallocated convert buffer with size: " + size );
		}
		
		if( _output == null || _output.capacity() < size ) {
			// We have to allocate buffer native, so we can pass it
			// to gl routines
			_output = NativeBuffers.allocateByte( size );
			LOGV( "Reallocated output buffer with size: " + size );
		}
		
		// We process image twice so display twice
		progress.setMax( pixels * 2 );
		
		_input.rewind();
		
		//convertRGB565to888( frame, _input );
		convertYUV420_NV21toRGB888( frame, width, height, _input, progress );
		//convolute( width, height, kernel, progress );
		
		_input.rewind();
		_output.rewind();
		_output.put( _input );

		_output.rewind();
		_output.limit( size );
		
		renderListener.onRendered( _output, width, height );
/*
		{
			Bitmap bitmap = Bitmap.createBitmap( width, height, Config.ARGB_8888 );
			for( int x = 0; x < width; x++ ) {
				for( int y = 0; y < height; y++ ) {
					int color = 0xFF << 24
					          | (_output.get() & 0xFF) << 16
					          | (_output.get() & 0xFF) <<  8
					          | (_output.get() & 0xFF) <<  0;
					bitmap.setPixel(x, y, color);
				}
			}
			
			FileOutputStream out;
			try {
				out = new FileOutputStream( _cacheDir + "/picture.png" );
			} catch (FileNotFoundException e) {
				throw new RuntimeException( e );
			}
			bitmap.compress( Bitmap.CompressFormat.PNG, 90, out );
			try {
				out.flush();
			} catch (IOException e1) {
			}
			try {
				out.close();
			} catch (IOException e) {
			}
		}

		GLES20Texture.glTexImage2D( GLES20.GL_RGB, width / 4, height / 4, GLES20.GL_UNSIGNED_BYTE, _output );
		GLUtils.throwOnError( "glEGLImageTargetTexture2DOES" );
*/
	}
		
	private void convolute( int width, int height, int[] kernel, ProgressListener progress ) {
		
		for( int y = 0; y < height; ++y ) {
			for (int x = 0; x < width; ++x ) {
/*
					X X X X X
					X X X X X
					X X X X X
					X X X X X
					X X X X X
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

				setPixel( _output, x, y, width, (byte)r, (byte)g, (byte)b );
			}
			progress.incrementBy( width );
			
		}
	}

}
