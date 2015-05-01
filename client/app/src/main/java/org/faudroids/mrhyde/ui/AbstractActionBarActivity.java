package org.faudroids.mrhyde.ui;

import javax.inject.Inject;

import roboguice.activity.RoboActionBarActivity;
import rx.subscriptions.CompositeSubscription;


abstract class AbstractActionBarActivity extends RoboActionBarActivity {

	protected final CompositeSubscription compositeSubscription = new CompositeSubscription();
	@Inject UiUtils uiUtils;

	@Override
	public void onDestroy() {
		super.onDestroy();
		compositeSubscription.unsubscribe();
	}

}
