package org.faudroids.mrhyde.ui.utils;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import timber.log.Timber;

/**
 * On some devices the scollTo method is called with leads to an UnsupportedOperationException.
 * This class tries to fix that.
 * Courtesy to http://stackoverflow.com/a/28432632
 */
public class UnscrollableRecyclerView extends RecyclerView {

	public UnscrollableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public UnscrollableRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UnscrollableRecyclerView(Context context) {
		super(context);
	}

	@Override
	public void scrollTo(int x, int y) {
		Timber.w("scrolling not supported");
	}

}
