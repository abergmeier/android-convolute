package de.hsbremen.android.convolution.gl;

import java.nio.Buffer;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

// Class, which hides selection of texture target
public abstract class GLES20Texture {

	private static final int GL_TEXTURE_EXTERNAL_OES = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
	private static final int TARGET = GLES20.GL_TEXTURE_2D;
	private static boolean use_external() {
		return TARGET == GL_TEXTURE_EXTERNAL_OES;
	}
	
	public static void glTexImage2D(int format, int width, int height, int type, Buffer pixels) {
		if( use_external() )
			GLES11Ext.glEGLImageTargetTexture2DOES( TARGET, pixels );
		else
			GLES20.glTexImage2D( TARGET, 0, format, width, height, 0, format, type, pixels);
	}
	
	public static void glBindTexture( int samplerLocation ) {
		GLES20.glBindTexture( TARGET, samplerLocation );
	}

	public static void glTexParameteri( int pname, int param) {
		GLES20.glTexParameteri( TARGET, pname, param );
	}

	public static String shaderRequire() {
		if( use_external() )
			return "#extension GL_OES_EGL_image_external : require";
		else
			return "";
	}

	public static String samplerType() {
		if( use_external() )
			return "samplerExternalOES";
		else
			return "sampler2D";
	}
}
