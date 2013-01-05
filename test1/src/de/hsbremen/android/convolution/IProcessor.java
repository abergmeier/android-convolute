package de.hsbremen.android.convolution;

import java.nio.ByteBuffer;


public interface IProcessor {
	void process( ByteBuffer frame,
	              int width, int height,
	              int[] kernel,
	              ProgressListener progress, RenderListener renderListener );
}
