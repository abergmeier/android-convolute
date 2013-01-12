package de.hsbremen.android.convolution;

import android.widget.ProgressBar;

public class ProgressDelegator
implements ProgressListener {
	private ProgressBar _progressBar;
	
	public ProgressDelegator( ProgressBar progressBar ) {
		_progressBar = progressBar;
	}

	@Override
	public void incrementBy(int i) {
		if( _progressBar != null )
			_progressBar.incrementProgressBy( i );
	}

	@Override
	public int getMax() {
		if( _progressBar != null )
			return _progressBar.getMax();
		return 0;
	}

	@Override
	public void setMax(int max ) {
		if( _progressBar != null )
			_progressBar.setMax( max );
	}

	@Override
	public void reset() {
		if( _progressBar != null )
			_progressBar.setProgress( 0 );
	}
	
	@Override
	public void close() {
		_progressBar = null;
	}
}
