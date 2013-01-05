package de.hsbremen.android.convolution.gl;

import java.nio.FloatBuffer;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView.SurfaceTextureListener;
import de.hsbremen.android.convolution.CameraProcessor;
import de.hsbremen.android.convolution.IProcessor;
import de.hsbremen.android.convolution.NativeBuffers;
import de.hsbremen.android.convolution.ProgressListener;
import de.hsbremen.android.convolution.RenderListener;
import de.hsbremen.android.convolution.Renderer;

public abstract class GLRenderer<PROC extends IProcessor>
extends Renderer<PROC>
implements CameraProcessor.Listener, SurfaceTextureListener {
	private static final String LOG_TAG = GLRenderer.class.getSimpleName();
	private static final String VERTEX_SHADER = "attribute vec4 a_position;  \n"
	                                          + "attribute vec2 a_texCoord;  \n"
	                                          + "varying vec2 v_texCoord;    \n"
	                                          + "void main() {               \n"
	                                          + "  gl_Position = a_position; \n"
	                                          + "  v_texCoord  = a_texCoord; \n"
	                                          + "}                           \n";

	private static final String FRAGMENT_SHADER = GLES20Texture.shaderRequire() +                        "\n"
	                                            + "precision mediump float;                               \n"
	                                            + "varying vec2 v_texCoord;                               \n"
	                                            + "uniform " + GLES20Texture.samplerType() + " s_texture; \n"
	                                            + "void main() {                                          \n"
	                                            + "  gl_FragColor = texture2D( s_texture, v_texCoord );   \n"
	                                            + "}                                                      \n";
	
	private static final FloatBuffer VERTICES = createVertices();
	
	private static FloatBuffer createVertices() {
		float[] values = new float[] { -0.5f, +0.5f, 0.0f,
		                               -0.5f, -0.5f, 0.0f,
		                               +0.5f, -0.5f, 0.0f,
		                               +0.5f, +0.5f, 0.0f };
		final FloatBuffer vertices = NativeBuffers.allocateFloat( values.length );
		vertices.put( values );
		return vertices;
	}
	
	private static final FloatBuffer TEX_COORDS = createTexCoords();
	
	private static FloatBuffer createTexCoords() {
		float[] values = new float[] { 0.0f, 0.0f,
		                               0.0f, 1.0f,
		                               1.0f, 1.0f,
		                               1.0f, 0.0f };
		
		final FloatBuffer texCoords = NativeBuffers.allocateFloat( values.length );
		texCoords.put( values );
		return texCoords;
	}
	
	private int _surfaceWidth,
				_surfaceHeight;
	
	public GLRenderer( PROC processor, ProgressListener progress, RenderListener renderListener ) {
		super( processor, progress, renderListener );
	}
	
	protected static void LOGD( String message ) {
		Log.d( LOG_TAG, message );
	}
	
	protected static void LOGV( String message ) {
		Log.v( LOG_TAG, message );
	}
	
	private static void throwError( String message ) {
		GLUtils.throwOnError( message );
	}
	
	private static void logError( String message ) {
		GLUtils.logOnError( message );
	}
	
	// Process in own thread so main thread is not blocked
	public class RenderHandler {
		// Handle to a program object
		protected int _program,
		              _samplerLocation,
		              _vertexPosition,
		              _textureCoord;
		
		private final int[] _textureId = new int[1];
/*		
		private class Framebuffers {
			private static final int COUNT = 1;
			private        final int[] _buffers = new int[]{0};
			private static final int OFFSET = 0;
			
			public Framebuffers() {
				GLES20.glGenFramebuffers( COUNT, _buffers, OFFSET );
				throwError( "glGenFramebuffers" );
			}
			
			public void bind(int index) {
				GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, _buffers[OFFSET + index] );
				throwError( "glBindFramebuffer" );
			}
			
			public void attach( Renderbuffers renderbuffers, int index, int attachment ) {
				GLES20.glFramebufferRenderbuffer( GLES20.GL_FRAMEBUFFER, attachment, GLES20.GL_RENDERBUFFER, renderbuffers.get(index) );
				throwError( "glFramebufferTexture2D" );
			}
			
			public void close() {
				GLES20.glDeleteFramebuffers( COUNT, _buffers, OFFSET );
				logError( "glDeleteFramebuffers" );
			}
		}

		private class Renderbuffers {
			private static final int COUNT = 1;
			private        final int[] _buffers = new int[]{0};
			private static final int OFFSET = 0;
			
			public Renderbuffers() {
				GLES20.glGenRenderbuffers( COUNT, _buffers, OFFSET );
				throwError( "glGenRenderbuffers" );
			}
			
			public int get( int index ) {
				return _buffers[index];
			}
			
			public void close() {
				GLES20.glDeleteRenderbuffers( COUNT, _buffers, OFFSET );
				logError( "glDeleteRenderbuffers" );
			}
		}
*/
		private final SurfaceTexture _surface;
		private       EGLSetup       _egl;
		
		public RenderHandler( SurfaceTexture surface ) {
			_surface = surface;
		}
		
		public final Runnable init = new Runnable() {
			@Override
			public void run() {
				_egl = new EGLSetup( _surface );
				_egl.setCurrent();
				/*
			
				// Load the shaders and get a linked program object
				_program = ESShader.loadProgram( VERTEX_SHADER, FRAGMENT_SHADER );

				// Get the attribute locations
				_vertexPosition  = GLES20.glGetAttribLocation ( _program, "a_position" );
				throwError( "glGetAttributeLocation" );
				_textureCoord    = GLES20.glGetAttribLocation ( _program, "a_texCoord" );
				throwError( "glGetAttributeLocation" );
				_samplerLocation = GLES20.glGetUniformLocation( _program, "s_texture"  );
				throwError( "glGetUniformLocation" );

				GLES20.glClearColor( 0.0f, 0.0f, 0.0f, 0.0f );
				throwError( "glClearColor" );
				
				// Use tightly packed data
				GLES20.glPixelStorei ( GLES20.GL_UNPACK_ALIGNMENT, 1 );
				throwError( "glPixelStorei" );
				
			//  Generate a texture object
				GLES20.glGenTextures ( 1, _textureId, 0 );
		
				// Set the filtering mode
				GLES20Texture.glTexParameteri( GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST );
				throwError( "glTexParameteri" );
				GLES20Texture.glTexParameteri( GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST );
				throwError( "glTexParameteri" );
*/
			}
		};
		
		public final Runnable preTexture = new Runnable() {
			@Override
			public void run() {
				_egl.setCurrent();
			
				// Use the program object
				GLES20.glUseProgram( _program );
				throwError( "glUseProgram (" + _program + ")" );
			
				// Set the viewport
				GLES20.glViewport( 0, 0, _surfaceWidth, _surfaceHeight );
				throwError( "glViewport" );

				// Clear the color buffer
				GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT );
				throwError( "glClear" );

				// Load the vertex data
				GLES20.glVertexAttribPointer( _vertexPosition, 3, GLES20.GL_FLOAT,
				                              false, 0, VERTICES );
				throwError( "glVertexAttribPointer" );
			
				GLES20.glEnableVertexAttribArray( _vertexPosition );
				throwError( "glEnableVertexAttribArray" );
				
				// Load texture data
				GLES20.glVertexAttribPointer ( _textureCoord, 2, GLES20.GL_FLOAT,
				                               false, 0, TEX_COORDS );
				throwError( "glVertexAttribPointer" );
				
				GLES20.glEnableVertexAttribArray( _textureCoord );
				throwError( "glEnableVertexAttribArray" );
			
				// Bind the texture unit 0
				GLES20.glActiveTexture( GLES20.GL_TEXTURE0 );
				throwError( "glActiveTexture" );
				
				GLES20Texture.glBindTexture( _textureId[0] );
				throwError( "glBindTexture" );
				
				// Set the sampler texture unit to 0
				GLES20.glUniform1i ( _samplerLocation, 0 );

			}
		};
		
		public final Runnable postTexture = new Runnable() {
			public void run() {
				// Draw quad (via triangles)
				GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP, 0, 4 );
				throwError( "glDrawArrays" );
				
				_egl.display();
			}
		};
		
		public final Runnable quit = new Runnable() {
			@Override
			public void run() {
				if( _egl != null ) {
					_egl.close();
				}
			}
		};
	}

	///
	// Handle surface changes
	//
	public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surface, final int width, final int height) {
		_surfaceWidth  = width;
		_surfaceHeight = height;
	}
}