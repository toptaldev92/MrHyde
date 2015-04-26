package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import roboguice.fragment.provided.RoboFragment;
import rx.subscriptions.CompositeSubscription;


abstract class AbstractFragment extends RoboFragment {

	private final int layoutResource;
	protected CompositeSubscription compositeSubscription = new CompositeSubscription();

	AbstractFragment(int layoutResource) {
		this.layoutResource = layoutResource;
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
