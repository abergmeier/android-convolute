package de.hsbremen.android.convolution.java;

import de.hsbremen.android.convolution.ProgressListener;
import de.hsbremen.android.convolution.RenderListener;

public class Renderer
extends de.hsbremen.android.convolution.Renderer<Processor> {

	public Renderer( ProgressListener progress, RenderListener renderListener ) {
		super( new Processor(), progress, renderListener);
	}
}
