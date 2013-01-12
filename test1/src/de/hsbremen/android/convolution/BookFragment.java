package de.hsbremen.android.convolution;

import android.content.Intent;
import android.net.Uri;

public class BookFragment
extends NamedFragment {
	public BookFragment() {
		super( R.string.title_books );
	}
	
	private void openWebPage() {
		startActivity( new Intent( Intent.ACTION_VIEW,
		                           Uri.parse("http://www.google.com") ) );
	}
}
