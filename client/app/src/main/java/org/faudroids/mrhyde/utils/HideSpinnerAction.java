package org.faudroids.mrhyde.utils;


import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.AbstractFragment;

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
		if (activity != null && activity.isSpinnerVisible()) activity.hideSpinner();
		if (fragment != null && fragment.isSpinnerVisible()) fragment.hideSpinner();
	}

}
