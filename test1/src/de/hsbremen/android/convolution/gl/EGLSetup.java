package de.hsbremen.android.convolution.gl;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

import android.graphics.SurfaceTexture;
import android.util.Log;

public class EGLSetup {
	private static class CurrentException
	extends RuntimeException {
		public CurrentException( String message ) {
			super( message );
		}
	}
	private static final int EGL_OPENGL_ES2_BIT = 4;
	private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	private final EGL10 _egl;
	private final EGLDisplay _display;
	private final EGLConfig  _config;
	private final EGLContext _context;
	private final EGLSurface _surface;
	private final String LOG_TAG = EGLSetup.class.getSimpleName();

	public EGLSetup( SurfaceTexture windowSurface ) {
		_egl = (EGL10)EGLContext.getEGL();
		_display = _egl.eglGetDisplay( EGL10.EGL_DEFAULT_DISPLAY );
		if( _display == EGL10.EGL_NO_DISPLAY )
			throw new RuntimeException("eglGetDisplay failed " + getErrorMessage());

		int[] version = new int[2];
		if( !_egl.eglInitialize( _display, version ) )
			throw new RuntimeException("eglInitialize failed " + getErrorMessage());

		_config = chooseEglConfig();
		if( _config == null )
			throw new RuntimeException("eglConfig not initialized");

		_context = createContext( _egl, _display, _config );

		_surface = _egl.eglCreateWindowSurface( _display, _config, windowSurface, null );

		if( _surface == null || _surface == EGL10.EGL_NO_SURFACE ) {
			int error = _egl.eglGetError();
			if( error == EGL10.EGL_BAD_NATIVE_WINDOW ) {
				Log.e( LOG_TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW." );
				return;
			}
			throw new RuntimeException( "createWindowSurface failed " + getErrorMessage(error) );
		}
	}
	
	public void setCurrent() {
		if( !_egl.eglMakeCurrent( _display, _surface, _surface, _context ) ) {
			throw new CurrentException( "eglMakeCurrent failed " + getErrorMessage() );
		}
	}
	
	public void close() {
		_egl.eglMakeCurrent( null, null, null, null );
		_egl.eglDestroySurface( _display, _surface );
		_egl.eglDestroyContext( _display, _context );
	}
	
	private EGLContext createContext( EGL10 egl, EGLDisplay display, EGLConfig config ) {
		int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
		return egl.eglCreateContext( display, config, EGL10.EGL_NO_CONTEXT, attrib_list );
	}
	
	private EGLConfig chooseEglConfig() {
		int[] configsCount = new int[1];
		EGLConfig[] configs = new EGLConfig[1];
		int[] configSpec = getConfig();
		if( !_egl.eglChooseConfig( _display, configSpec, configs, 1, configsCount) )
			throw new IllegalArgumentException("eglChooseConfig failed " + getErrorMessage() );
		else if( configsCount[0] > 0 )
			return configs[0];
		else
			return null;
	}
	
	private int[] getConfig() {
		return new int[] {
			EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
			EGL10.EGL_RED_SIZE, 8,
			EGL10.EGL_GREEN_SIZE, 8,
			EGL10.EGL_BLUE_SIZE, 8,
			//EGL10.EGL_ALPHA_SIZE, 8, I don't care about alpha
			EGL10.EGL_DEPTH_SIZE, 0,
			EGL10.EGL_STENCIL_SIZE, 0,
			EGL10.EGL_NONE
		};
	}
	
	public void display() {
		_egl.eglSwapBuffers( _display, _surface );
	}
	
	private static String getErrorMessage( int error ) {
		return android.opengl.GLUtils.getEGLErrorString(error);
	}
	
	private String getErrorMessage() {
		return getErrorMessage( _egl.eglGetError() );
	}
	
	public GL getGL() {
		return _context.getGL();
	}
}
