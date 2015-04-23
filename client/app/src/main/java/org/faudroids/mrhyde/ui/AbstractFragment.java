package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import roboguice.fragment.provided.RoboFragment;
import rx.subscriptions.CompositeSubscription;


abstract class AbstractFragment extends RoboFragment {

	private final int layoutResource;
	@Inject UiUtils uiUtils;
	protected final CompositeSubscription compositeSubscription = new CompositeSubscription();
	protected ActionBarListener actionBarListener;

	AbstractFragment(int layoutResource) {
		this.layoutResource = layoutResource;
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		actionBarListener = UiUtils.activityToActionBarListener(activity);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(layoutResource, container, false);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		compositeSubscription.unsubscribe();
	}

}
