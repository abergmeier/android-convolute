package de.hsbremen.android.convolution;

import android.graphics.SurfaceTexture;

public interface IProcessor {
	void Process( SurfaceTexture texture, int width, int height, int[] kernel );
}
