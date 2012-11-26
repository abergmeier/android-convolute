package de.hsbremen.android.convolution.java;

import java.nio.Buffer;
import java.nio.LongBuffer;

import com.example.test1.TextureId;

import de.hsbremen.android.convolution.IProcessor;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

public class Processor
implements IProcessor {
	private LongBuffer _buffer;
	        LongBuffer _output;
	private int _width, _height;
	
	public Processor() {
		_buffer = LongBuffer.allocate( _width * _height );
		_output = LongBuffer.allocate( _width * _height );
	}
	
	private int getOffset( int x, int y ) {
		return x + y * _width;
	}
	
	private Long getPixel( int x, int y ) {
		if( x < 0 || x >= _width || y < 0 || y <= _height )
			return null;
		
		final int offset = getOffset( x, y );
		return _buffer.array()[_buffer.arrayOffset() + offset];
	}
	
	private void setPixel( Buffer pixels, int x, int y, int color ) {
		final int offset = getOffset( x, y );
		long[] buffer = (long[])pixels.array();
		buffer[pixels.arrayOffset() + offset] = color;
	}
	
	void onDrawFrame() {
		
	}
	
	private static int clip( int color ) {
		return Math.max(Math.min(color, 255), 0);
	}

	private static int rgb(int r, int g, int b) {
		return 0xFF000000 |
		       ((r & 0xFF) << 16) |
		       ((g & 0xFF) <<  8) |
		       ((b & 0xFF) <<  0);
	}

	private static short red  (int color) { return (short)((color >> 16) & 0xFF); }
	private static short green(int color) { return (short)((color >>  8) & 0xFF); }
	private static short blue (int color) { return (short)((color >>  0) & 0xFF); }
	
	private static short red  (Long color) { return (short)((color.longValue() >> 16) & 0xFF); }
	private static short green(Long color) { return (short)((color.longValue() >>  8) & 0xFF); }
	private static short blue (Long color) { return (short)((color.longValue() >>  0) & 0xFF); }
	
	public void Process(SurfaceTexture texture, int width, int height, int[] kernel ) {
		GLES20.glReadPixels( 0, 0, _width, _height, GLES20.GL_RGB, GLES20.GL_UNSIGNED_INT, _buffer );
		
		for( int y = 0; y < _height; ++y ) {
			for (int x = 0; x < _width; ++x ) {
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

				for( int dx = -2; dx != 3; ++dx ) {
					for( int dy = -2; dy != 3; ++dy ) {
						final int kern = kernel[(2 + dx) + (2 + dy) * 5];
						Long pixel = getPixel( x + dx, y + dy );
						if( pixel == null )
							continue;
						
						r += red  (pixel) * kern;
						g += green(pixel) * kern;
						b += blue (pixel) * kern;
						r = clip(r);
						g = clip(g);
						b = clip(b);
					}
				}
				setPixel( _output, x, y, rgb(r, g, b));
			}
		}
	}

}
