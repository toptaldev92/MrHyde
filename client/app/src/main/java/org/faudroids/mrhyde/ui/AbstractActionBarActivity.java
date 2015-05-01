package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import javax.inject.Inject;

import roboguice.activity.RoboActionBarActivity;
import rx.subscriptions.CompositeSubscription;


abstract class AbstractActionBarActivity extends RoboActionBarActivity {

	protected final CompositeSubscription compositeSubscription = new CompositeSubscription();
	@Inject UiUtils uiUtils;

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

}
