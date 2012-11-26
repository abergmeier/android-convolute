package de.hsbremen.android.convolution.nio;

import de.hsbremen.android.convolution.nio.Processor;

public class Renderer
extends de.hsbremen.android.Renderer {
	public Renderer() {
		super( new Processor() );
	}

	@Override
	protected void onDrawFrame() {
	}
}
