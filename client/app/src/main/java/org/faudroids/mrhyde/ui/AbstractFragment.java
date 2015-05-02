package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.faudroids.mrhyde.R;

import javax.inject.Inject;

import roboguice.fragment.provided.RoboFragment;
import roboguice.inject.InjectView;
import rx.subscriptions.CompositeSubscription;


public abstract class AbstractFragment extends RoboFragment {

	private final int layoutResource;
	protected CompositeSubscription compositeSubscription = new CompositeSubscription();
	@Inject UiUtils uiUtils;
	@InjectView(R.id.spinner) View spinnerContainerView;
	@InjectView(R.id.spinner_image) ImageView spinnerImageView;

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


	public void showSpinner() {
		uiUtils.showSpinner(spinnerContainerView, spinnerImageView);
	}


	public void hideSpinner() {
		uiUtils.hideSpinner(spinnerContainerView, spinnerImageView);
	}

}
