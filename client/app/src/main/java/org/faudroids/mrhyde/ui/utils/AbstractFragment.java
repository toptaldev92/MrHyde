package org.faudroids.mrhyde.ui.utils;

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
	@Inject protected UiUtils uiUtils;
	@InjectView(R.id.spinner) protected View spinnerContainerView;
	@InjectView(R.id.spinner_image) protected ImageView spinnerImageView;

	protected AbstractFragment(int layoutResource) {
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


	public boolean isSpinnerVisible() {
		return uiUtils.isSpinnerVisible(spinnerContainerView);
	}

}
