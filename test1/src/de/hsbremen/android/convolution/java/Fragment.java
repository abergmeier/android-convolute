package de.hsbremen.android.convolution.java;

import android.app.Activity;

import de.hsbremen.android.Renderer;

public class Fragment
extends de.hsbremen.android.convolution.Fragment {
	
	public Fragment() {
		final Activity activity = getActivity();
		_surfaceView.setRenderer(new Renderer(activity) );
	}
	
	

}
