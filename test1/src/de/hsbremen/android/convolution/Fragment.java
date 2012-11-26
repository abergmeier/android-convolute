package de.hsbremen.android.convolution;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public abstract class Fragment
extends android.app.Fragment {
	protected GLSurfaceView _surfaceView;
	protected final IProcessor _convoluter;
	
	protected Fragment( ) {
		if( !ensureOpenGLES20() ) {
			throw new RuntimeException("OpenGL ES 2.0 needed but not supported.");
		}
		_surfaceView = new GLSurfaceView( getActivity() );
		_surfaceView.setEGLContextClientVersion( 2 );
	}
	
	private boolean ensureOpenGLES20() 
	{
		ActivityManager am = (ActivityManager)getActivity().getSystemService( Context.ACTIVITY_SERVICE );
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		return info.reqGlEsVersion >= 0x20000;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.article_view, container, false);
	}
}
