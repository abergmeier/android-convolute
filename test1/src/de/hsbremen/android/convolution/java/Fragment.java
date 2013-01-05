package de.hsbremen.android.convolution.java;

import de.hsbremen.android.convolution.ProgressListener;
import de.hsbremen.android.convolution.RenderListener;
import android.widget.ImageView;

public class Fragment
extends de.hsbremen.android.convolution.Fragment {
	
	@Override
	protected Renderer createRenderer( ProgressListener progressListener,
	                                   RenderListener renderListener ) {
		return new Renderer( progressListener, renderListener );
	}
}
