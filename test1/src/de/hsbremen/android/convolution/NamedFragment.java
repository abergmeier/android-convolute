package de.hsbremen.android.convolution;

import android.app.Fragment;

public abstract class NamedFragment
extends Fragment {
	private final int _stringId;
	protected NamedFragment( int stringId ) {
		_stringId = stringId;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle( _stringId );
	}
}
