package de.hsbremen.android.convolution;

import java.nio.ByteBuffer;

public interface RenderListener {

	void onRendered( ByteBuffer output, int width, int height );

}
