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
	protected CompositeSubscription compositeSubscription = new CompositeSubscription();
	protected ActivityListener activityListener;

	AbstractFragment(int layoutResource) {
		this.layoutResource = layoutResource;
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityListener = UiUtils.activityToActionBarListener(activity);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(layoutResource, container, false);
	}


	@Override
	public void onDestroy() {
		compositeSubscription.unsubscribe();
		compositeSubscription = new CompositeSubscription();
		super.onDestroy();
	}

}
