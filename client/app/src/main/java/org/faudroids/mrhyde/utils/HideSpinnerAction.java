package org.faudroids.mrhyde.utils;


import org.faudroids.mrhyde.ui.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.AbstractFragment;

public class HideSpinnerAction extends AbstractErrorAction {

	private final AbstractActionBarActivity activity;
	private final AbstractFragment fragment;

	public HideSpinnerAction(AbstractActionBarActivity activity) {
		this.activity = activity;
		this.fragment = null;
	}

	public HideSpinnerAction(AbstractFragment fragment) {
		this.activity = null;
		this.fragment = fragment;
	}


	@Override
	protected void doCall(Throwable throwable) {
		if (activity != null) activity.hideSpinner();
		if (fragment != null) fragment.hideSpinner();
	}

}
