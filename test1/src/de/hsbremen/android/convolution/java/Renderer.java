package de.hsbremen.android.convolution.java;

import javax.microedition.khronos.opengles.GL10;

import de.hsbremen.android.convolution.IProcessor;

import android.opengl.GLES20;

public class Renderer
extends de.hsbremen.android.Renderer {
	
	public Renderer() {
		super( new Processor() );
	}

	///
	// Draw a triangle using the shader pair created in onSurfaceCreated()
	@Override
	protected void onUpdate() {
	}

	@Override
	protected void onDrawFrame() {
		// Set the viewport
		GLES20.glViewport( 0, 0, _surfaceWidth, _surfaceHeight );

		// Clear the color buffer
		GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT );

		// Use the program object
		GLES20.glUseProgram( _program );

		
		// Load the vertex data
		GLES20.glVertexAttribPointer( _vertexPosition, 3, GLES20.GL_FLOAT,
		                              false, 0, mCube.getVertices() );
		GLES20.glEnableVertexAttribArray( _vertexPosition );

		// Load the MVP matrix
		GLES20.glUniformMatrix4fv( mMVPLoc, 1, false,
		                           mMVPMatrix.getAsFloatBuffer() );

		// Draw the cube
		GLES20.glDrawElements( GLES20.GL_TRIANGLES, mCube.getNumIndices(),
		                       GLES20.GL_UNSIGNED_SHORT, mCube.getIndices());
	}
}
