package de.hsbremen.android.convolution.gl;

import android.opengl.GLES20;
import android.util.Log;

public class GLUtils {
	private static String getErrorMessage() {
		int error = GLES20.glGetError();
		
		String errorMessage = null;
		switch( error ) {
		case GLES20.GL_NO_ERROR:
			break;
		case GLES20.GL_INVALID_ENUM:
			errorMessage = "GL_INVALID_ENUM";
			break;
		case GLES20.GL_INVALID_VALUE:
			errorMessage = "GL_INVALID_VALUE";
			break;
		case GLES20.GL_INVALID_OPERATION:
			errorMessage = "GL_INVALID_OPERATION";
			break;
		default:
			errorMessage = String.valueOf(error);
			break;
		}
		return errorMessage;
	}
	
	public static void throwOnError( String message ) {
		final String errorMessage = getErrorMessage();
		
		if( errorMessage != null )
			throw new RuntimeException( message + " failed with: " + errorMessage );
	}
	
	public static void logOnError( String message ) {
		final String errorMessage = getErrorMessage();
		
		if( errorMessage != null )
			Log.d( "DEBUG", errorMessage );
	}
}
