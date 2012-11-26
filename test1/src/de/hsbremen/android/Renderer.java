package de.hsbremen.android;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;


import de.hsbremen.android.convolution.IProcessor;

public abstract class Renderer
implements GLSurfaceView.Renderer {
	// Handle to a program object
	protected int _program,
	              _samplerLocation,
	              _surfaceWidth,
	              _surfaceHeight;

	private Camera         _camera;
	private SurfaceTexture _texture;
	private IProcessor     _processor;
	private int[]          _kernel;
	
	public Renderer( IProcessor processor ) {
		_processor = processor;
	}
	
	//
	// Initialize the shader and program object
	//
	@Override
	public void onSurfaceCreated( GL10 glUnused, EGLConfig config ) {
		{
			String vertexShader = "attribute vec4 a_position;  \n"
			                    + "void main() {               \n"
			                    + "  gl_Position = a_position; \n"
			                    + "}                           \n";
	
			String fragmentShader = "precision mediump float;                             \n"
			                      + "varying vec2 v_texCoord;                             \n"
			                      + "uniform sampler2D s_texture;                         \n"
			                      + "void main() {                                        \n"
			                      + "  gl_FragColor = texture2D( s_texture, v_texCoord ); \n"
			                      + "}                                                    \n";
	
			
			// Load the shaders and get a linked program object
			_program = ESShader.loadProgram( vertexShader, fragmentShader );
		}

		//Get the attribute locations
		_vertexPosition  = GLES20.glGetAttribLocation ( _program, "a_position" );
		_samplerLocation = GLES20.glGetUniformLocation( _program, "s_texture"  );

		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		
		_camera = Camera.open(0);
		try {
			_camera.setPreviewTexture( _texture );
		} catch( IOException ex ) {
			throw new RuntimeException( ex );
		}
	}

	///
	// Handle surface changes
	//
	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		_surfaceWidth = width;
		_surfaceHeight = height;
	}
	
	@Override
	public final void onDrawFrame( GL10 glUnused ) {
		onUpdate();
		onDrawFrame();
	}
	
	protected abstract void onDrawFrame();
	
	protected void onUpdate() {
		_texture.updateTexImage();
		_processor.Process(_texture, _surfaceWidth, _surfaceHeight, _kernel );
	}

	// Attribute locations
	private int _vertexPosition;

	// Uniform locations
}