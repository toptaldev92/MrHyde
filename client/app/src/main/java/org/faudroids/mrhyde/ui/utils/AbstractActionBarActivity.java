package org.faudroids.mrhyde.ui.utils;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import org.faudroids.mrhyde.R;

import javax.inject.Inject;

import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;
import rx.subscriptions.CompositeSubscription;


public abstract class AbstractActionBarActivity extends RoboActionBarActivity {

	protected final CompositeSubscription compositeSubscription = new CompositeSubscription();
	@Inject protected UiUtils uiUtils;
	@InjectView(R.id.spinner) protected View spinnerContainerView;
	@InjectView(R.id.spinner_image) protected ImageView spinnerImageView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// show action bar back button
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle action bar back buttons
		switch(item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;

		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		compositeSubscription.unsubscribe();
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
