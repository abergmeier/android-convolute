package de.hsbremen.android.convolution.nio;

import de.hsbremen.android.convolution.ProgressListener;
import de.hsbremen.android.convolution.RenderListener;

public class Fragment
extends de.hsbremen.android.convolution.ConvolutionFragment {
	
	@Override
	protected Renderer createRenderer( ProgressListener progressListener, RenderListener renderListener ) {
		return new Renderer( progressListener, renderListener );
	}
}
