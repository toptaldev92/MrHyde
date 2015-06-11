package org.faudroids.mrhyde.ui.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

public class TintOverlayView extends ViewGroup {

	public TintOverlayView(Context context) {
		super(context);
	}

	public TintOverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TintOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) { }

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		onTouchEvent(event);
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (getAlpha() == 1) return super.onTouchEvent(event);
		else return false;
	}

}
