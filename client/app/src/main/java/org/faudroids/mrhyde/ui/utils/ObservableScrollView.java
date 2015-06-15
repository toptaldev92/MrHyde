package org.faudroids.mrhyde.ui.utils;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class ObservableScrollView extends ScrollView {

	private OnScrollListener listener = null;


	public ObservableScrollView(Context context) {
		super(context);
	}

	public ObservableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ObservableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onScrollChanged(int l, int t, int oldL, int oldT) {
		super.onScrollChanged(l, t, oldL, oldT);
		if (listener != null) {
			listener.onScrollChanged(this, l, t, oldL, oldT);
		}
	}

	public void setOnScrollListener(OnScrollListener listener) {
		this.listener = listener;
	}

	public interface OnScrollListener {

		void onScrollChanged(ScrollView scrollView, int l, int t, int oldL, int oldT);

	}

}
