package de.hsbremen.android.convolution;

public interface ProgressListener {
	void incrementBy( int i );

	int getMax();
	void setMax(int i);

	void reset();
	void close();
}
