package de.hsbremen.android.convolution;

import android.widget.ProgressBar;

public class ProgressDelegator
implements ProgressListener {
	private final ProgressBar _progressBar;
	
	public ProgressDelegator( ProgressBar progressBar ) {
		_progressBar = progressBar;
	}

	@Override
	public void incrementBy(int i) {
		_progressBar.incrementProgressBy( i );
	}

	@Override
	public int getMax() {
		return _progressBar.getMax();
	}

	@Override
	public void setMax(int max ) {
		_progressBar.setMax( max );
	}

	@Override
	public void reset() {
		_progressBar.setProgress( 0 );
	}

}
